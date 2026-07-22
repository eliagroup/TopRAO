/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.crac;

import com.powsybl.contingency.ContingencyElement;
import com.powsybl.openrao.data.crac.io.network.parameters.CriticalElements;
import com.powsybl.openrao.data.crac.io.network.parameters.NetworkCracCreationParameters;
import com.toprao.toop.data.ToOpCnecResult;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public abstract class AbstractCnecFilteringSpecifier implements CracCreationSpecifier {

    private ToOpN1Definition n1Definition;
    private ToOpLfResult toOpLfResult;

    protected CracGenerationParameters cracGenerationParameters;
    private Integer cocbLimit;
    private static final String BASE_CASE_ID = "BASECASE";
    private static final Double CRITICAL_ELEMENTS_OPTIMIZED_MIN_V = null;

    protected void createFilteredCnecParameters(NetworkCracCreationParameters parameters) {
        Map<String, List<ToOpCnecResult>> selectedResultsByContingency = selectCnecs();
        List<String> selectedContingencyElementIds = selectedResultsByContingency.keySet().stream().flatMap(c -> n1Definition.getContingency(c).getElements().stream()).map(e -> e.getId()).toList();
        parameters.getContingencies().setBranchFilter(b -> selectedContingencyElementIds.contains(b.getId()));

        log.info("Contingency elements count : {}", selectedContingencyElementIds.size());

        parameters.getCriticalElements().setCountryFilter(CracGenerationParameters.ALL_COUNTRIES_SET);
        parameters.getCriticalElements().setOptimizedMinMaxV(CRITICAL_ELEMENTS_OPTIMIZED_MIN_V, null);
        parameters.getCriticalElements().setMonitoredMinMaxV(null);

        // TODO clean (e.g. selectedResultsByContingency -> key is contingency element -> use directly that in optimized predicate)
        Map<String, List<String>> cbco = new HashMap<>();
        selectedResultsByContingency.forEach((c, results) -> {
            var contingencyElements = n1Definition.getContingency(c).getElements();
            contingencyElements.forEach(ce -> results
                            .forEach(r -> cbco.computeIfAbsent(r.element(), re -> new ArrayList<>()).add(ce.getId()))
            );
        });

        List<String> monitoredBranchesIdsBaseCase = n1Definition.getMonitoredElements().stream().map(e -> e.getId()).toList();

        log.info("CBCO count : {}", cbco.entrySet().stream().flatMap(e -> e.getValue().stream()).toList().size());
        parameters.getCriticalElements().setOptimizedMonitoredProvider((branch, contingency, context) -> {
            boolean optimized;

            if (contingency != null) {
                optimized = cbco.containsKey(branch.getId())
                        && contingency.getElements().stream().map(ContingencyElement::getId).anyMatch(id -> cbco.get(branch.getId()).contains(id));
            } else {
                optimized = monitoredBranchesIdsBaseCase.contains(branch.getId());
            }
            return new CriticalElements.OptimizedMonitored(optimized, false);
        });
    }

    private Map<String, List<ToOpCnecResult>> selectCnecs() {
        if (cocbLimit != null) {
            return selectMostCriticalCnecs();
        }
        return selectCnecsByFlowDifWithBaseCase();
    }

    private Map<String, List<ToOpCnecResult>> selectMostCriticalCnecs() {
        List<String> n1DefContingencyIds = n1Definition.getContingencies().stream().map(c -> c.getId()).toList();
        List<String> n1DefContingencyElementsIds = n1Definition.getContingencies().stream().flatMap(c -> c.getElements().stream().map(e -> e.getId())).toList();

        return toOpLfResult.getResults()
                .stream()
                .filter(r -> n1DefContingencyElementsIds.contains(r.element()))
                .filter(r -> n1DefContingencyIds.contains(r.contingency()))
                .sorted(Comparator.comparingDouble(r -> -r.loading()))
                .limit(this.cocbLimit)
                .collect(Collectors.groupingBy(r -> r.contingency()));
    }

    private Map<String, List<ToOpCnecResult>> selectCnecsByFlowDifWithBaseCase() {
        List<String> n1DefContingencyIds = n1Definition.getContingencies().stream().map(c -> c.getId()).toList();
        List<String> monitoredElements = n1Definition.getMonitoredElements().stream().map(e -> e.getId()).toList();

        Map<String, ToOpCnecResult> baseCaseResults = toOpLfResult.getLfResultsForContingency(BASE_CASE_ID)
                .stream().collect(Collectors.toMap(r -> r.element(), r -> r));

        List<ToOpCnecResult> resultsOk = toOpLfResult.getResults()
                .stream()
                .filter(r -> n1DefContingencyIds.contains(r.contingency()))
                .filter(r -> monitoredElements.contains(r.element()))
                .filter(r -> isAffectedByContingency(r, baseCaseResults))
                .toList();
        return resultsOk.stream()
                .collect(Collectors.groupingBy(r -> r.contingency()));
    }

    private boolean isAffectedByContingency(ToOpCnecResult r, Map<String, ToOpCnecResult> baseCaseResults) {
        ToOpCnecResult baseCaseRes = baseCaseResults.get(r.element());
        if (baseCaseRes == null) {
            log.warn("No base case result for {}", r.element());
            return false;
        }

        return Math.abs(baseCaseRes.loading() - r.loading()) > cracGenerationParameters.getAffectedCnecMinLoadingDiff()
                && Math.abs(baseCaseRes.p() - r.p()) > cracGenerationParameters.getAffectedCnecMinActivePowerDiff();
    }
}
