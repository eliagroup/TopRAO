/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.sa;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.OperationalLimitsGroup;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.results.BranchResult;
import com.powsybl.security.results.NetworkResult;
import com.toprao.toop.data.ToOpCnecResult;
import com.toprao.toop.data.ToOpContingency;
import com.toprao.toop.data.ToOpGridElement;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SaResultsConverter {

    public SaResultsConverter() {
    }

    public ToOpLfResult convertResults(SecurityAnalysisResult saResult, Network network) {
        List<ToOpCnecResult> cnecResults = convertCnecs(saResult, network);
        return new ToOpLfResult(cnecResults);
    }

    public ToOpN1Definition convertN1Definition(SecurityAnalysisResult saResult, ToOpLfResult toOpLfResult) {
        Set<String> monitoredElementIds = toOpLfResult.getResults().stream().map(r -> r.element()).collect(Collectors.toSet());

        // Using powsybl contingencies to know the elements affected by the contingency (we don't have this in ToOpCnecResult)
        Set<Contingency> powsyblContingencies = saResult.getPostContingencyResults().stream().map(r -> r.getContingency()).collect(Collectors.toSet());

        // FIXME : name, type and kind are not used for the moment so this is fine, but would need to be added when those attributes will be useful
        List<ToOpGridElement> gridElements = monitoredElementIds.stream().map(e -> new ToOpGridElement(e, null, null, null)).toList();

        List<ToOpContingency> contingencies = new ArrayList<>(List.of(new ToOpContingency("BASECASE", "", List.of())));
        contingencies.addAll(powsyblContingencies.stream().map(c -> convertContingency(c)).toList());

        return new ToOpN1Definition(gridElements, contingencies, null);
    }

    private ToOpContingency convertContingency(Contingency contingency) {

        List<ToOpGridElement> gridElements = contingency.getElements().stream().map(e -> new ToOpGridElement(e.getId(), null, null, null)).toList();
        return new ToOpContingency(contingency.getId(), contingency.getName().orElse(null), gridElements);
    }

    private List<ToOpCnecResult> convertCnecs(SecurityAnalysisResult saResult, Network network) {
        List<ToOpCnecResult> cnecResults = new ArrayList<>(convertCnecs("BASECASE", saResult.getPreContingencyResult().getNetworkResult(), network));
        saResult.getPostContingencyResults().forEach(r -> cnecResults.addAll(convertCnecs(r.getContingency().getId(), r.getNetworkResult(), network)));
        return cnecResults;
    }

    private List<ToOpCnecResult> convertCnecs(String contingencyId, NetworkResult networkResult, Network network) {
        return networkResult.getBranchResults().stream().map(br -> convertCnec(contingencyId, br, network)).toList();
    }

    private ToOpCnecResult convertCnec(String contingencyId, BranchResult branchResult, Network network) {
        Branch<?> branch = getBranch(branchResult, network);
        double branchLoading1 = getBranchLoading1(branch, branchResult);

        int side;
        double p;
        double loading;

        if (!Double.isNaN(branchLoading1)) {
            side = 1;
            loading = branchLoading1;
            p = branchResult.getP1();
        } else {
            side = 2;
            loading = getBranchLoading2(branch, branchResult);
            p = branchResult.getP2();
        }
        return new ToOpCnecResult(branchResult.getBranchId(), contingencyId, side, loading, p);
    }

    private Branch<?> getBranch(BranchResult branchResult, Network network) {
        Branch<?> branch = network.getBranch(branchResult.getBranchId());
        if (branch != null) {
            return branch;
        }

        BoundaryLine boundaryLine = network.getBoundaryLine(branchResult.getBranchId());
        if (boundaryLine != null) {
            Optional<TieLine> tieLine = boundaryLine.getTieLine();
            if (tieLine.isPresent()) {
                return tieLine.get();
            }
        }
        throw new PowsyblException(MessageFormat.format("Branch {0} not found", branchResult.getBranchId()));
    }

    private double getBranchLoading1(Branch<?> branch, BranchResult branchResult) {
        Optional<OperationalLimitsGroup> l1opt = branch.getSelectedOperationalLimitsGroup1();
        if (l1opt.isPresent()) {
            OperationalLimitsGroup l1 = l1opt.get();
            if (l1.getCurrentLimits().isPresent()) {
                double limit = l1opt.get().getCurrentLimits().get().getPermanentLimit();
                double current = branchResult.getI1();
                return current / limit;
            }
        }
        return Double.NaN;
    }

    private double getBranchLoading2(Branch<?> branch, BranchResult branchResult) {
        Optional<OperationalLimitsGroup> l2opt = branch.getSelectedOperationalLimitsGroup2();
        if (l2opt.isPresent()) {
            OperationalLimitsGroup l1 = l2opt.get();
            if (l1.getCurrentLimits().isPresent()) {
                double limit = l2opt.get().getCurrentLimits().get().getPermanentLimit();
                double current = branchResult.getI2();
                return current / limit;
            }
        }
        return Double.NaN;
    }
}
