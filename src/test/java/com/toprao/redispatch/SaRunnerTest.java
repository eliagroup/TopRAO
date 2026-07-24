/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.redispatch;

import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.monitor.StateMonitor;
import com.toprao.sa.SaResultsConverter;
import com.toprao.sa.SecurityAnalysisRunner;
import com.toprao.toop.data.ToOpCnecResult;
import com.toprao.toop.data.ToOpGridElement;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import com.toprao.utils.NetworkImportsUtil;
import com.toprao.utils.TestAssertUtils;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SaRunnerTest {
    @Test
    void sa2nodesLfResult() {
        Network network = NetworkImportsUtil.import2NodesNetwork();
        ToOpN1Definition n1Definition = ToOpN1Definition.readFromInputStream(SaRunnerTest.class.getResourceAsStream("/redispatch/2nodes/n1_definition_2nodes.json"));

        SecurityAnalysisResult saResult = new SecurityAnalysisRunner().run(network, n1Definition);
        ToOpLfResult lfResult = new SaResultsConverter().convertResults(saResult, network);

        assertThat(lfResult.getResults()).hasSize(4);
        assertLfResult(lfResult, "BASECASE", "FRANCE_BELGIUM_1", 1, -266.6666, 0.5333518541025759);
        assertLfResult(lfResult, "BASECASE", "FRANCE_BELGIUM_2", 1, -533.333333, 1.0667);
        assertLfResult(lfResult, "CO_FRANCE_BELGIUM_1", "FRANCE_BELGIUM_2", 1, -800, 1.6);
        assertLfResult(lfResult, "CO_FRANCE_BELGIUM_2", "FRANCE_BELGIUM_1", 1, -800, 1.6);
    }

    @Test
    void sa2nodesLfResultWithPowsyblInputs() {
        Network network = NetworkImportsUtil.import2NodesNetwork();

        List<Contingency> contingencies = List.of(new Contingency("CO_FRANCE_BELGIUM_1", List.of(new BranchContingency("FRANCE_BELGIUM_1"))),
                new Contingency("CO_FRANCE_BELGIUM_2", List.of(new BranchContingency("FRANCE_BELGIUM_2"))));
        List<StateMonitor> stateMonitors = List.of(new StateMonitor(ContingencyContext.all(), Set.of("FRANCE_BELGIUM_1"), Set.of(), Set.of()),
                new StateMonitor(ContingencyContext.all(), Set.of("FRANCE_BELGIUM_2"), Set.of(), Set.of()));

        SecurityAnalysisResult saResult = new SecurityAnalysisRunner().run(network, contingencies, stateMonitors, SecurityAnalysisRunner.createLfParameters(), 1);
        ToOpLfResult lfResult = new SaResultsConverter().convertResults(saResult, network);

        lfResult.write(Path.of("branch_results.json"));

        assertThat(lfResult.getResults()).hasSize(4);
        assertLfResult(lfResult, "BASECASE", "FRANCE_BELGIUM_1", 1, -266.6666, 0.5333518541025759);
        assertLfResult(lfResult, "BASECASE", "FRANCE_BELGIUM_2", 1, -533.333333, 1.0667);
        assertLfResult(lfResult, "CO_FRANCE_BELGIUM_1", "FRANCE_BELGIUM_2", 1, -800, 1.6);
        assertLfResult(lfResult, "CO_FRANCE_BELGIUM_2", "FRANCE_BELGIUM_1", 1, -800, 1.6);
    }

    @Test
    void sa2nodesLfResultConvertedN1Def() {
        Network network = NetworkImportsUtil.import2NodesNetwork();
        List<Contingency> contingencies = List.of(new Contingency("CO_FRANCE_BELGIUM_1", List.of(new BranchContingency("FRANCE_BELGIUM_1"))),
                new Contingency("CO_FRANCE_BELGIUM_2", List.of(new BranchContingency("FRANCE_BELGIUM_2"))));
        List<StateMonitor> stateMonitors = List.of(new StateMonitor(ContingencyContext.all(), Set.of("FRANCE_BELGIUM_1"), Set.of(), Set.of()),
                new StateMonitor(ContingencyContext.all(), Set.of("FRANCE_BELGIUM_2"), Set.of(), Set.of()));

        SecurityAnalysisResult saResult = new SecurityAnalysisRunner().run(network, contingencies, stateMonitors, SecurityAnalysisRunner.createLfParameters(), 1);
        ToOpLfResult toOpLfResult = new SaResultsConverter().convertResults(saResult, network);
        ToOpN1Definition convertedN1Def = new SaResultsConverter().convertN1Definition(saResult, toOpLfResult);

        assertThat(convertedN1Def.getMonitoredElements()).hasSize(2);
        assertThat(convertedN1Def.getContingencies()).hasSize(3);

        ToOpGridElement convertedGridElement1 = convertedN1Def.getMonitoredElements().get(0);
        ToOpGridElement convertedGridElement2 = convertedN1Def.getMonitoredElements().get(1);

        // order of elements is not preserved
        assertThat(convertedGridElement1.getId()).isEqualTo("FRANCE_BELGIUM_2");
        // not implemented yet
        assertThat(convertedGridElement1.getName()).isNull();
        assertThat(convertedGridElement1.getType()).isNull();
        assertThat(convertedGridElement1.getKind()).isNull();

        assertThat(convertedGridElement2.getId()).isEqualTo("FRANCE_BELGIUM_1");
        assertThat(convertedGridElement2.getName()).isNull();
        assertThat(convertedGridElement2.getType()).isNull();
        assertThat(convertedGridElement2.getKind()).isNull();

        assertThat(convertedN1Def.getContingencies().get(0).getId()).isEqualTo("BASECASE");
        assertThat(convertedN1Def.getContingencies().get(0).getElements()).isEmpty();

        assertThat(convertedN1Def.getContingencies().get(1).getId()).isEqualTo("CO_FRANCE_BELGIUM_1");
        assertThat(convertedN1Def.getContingencies().get(1).getElements()).hasSize(1);
        TestAssertUtils.assertGridElementsIdentical(convertedN1Def.getContingencies().get(1).getElements().get(0), convertedGridElement2);

        assertThat(convertedN1Def.getContingencies().get(2).getId()).isEqualTo("CO_FRANCE_BELGIUM_2");
        assertThat(convertedN1Def.getContingencies().get(2).getElements()).hasSize(1);
        TestAssertUtils.assertGridElementsIdentical(convertedN1Def.getContingencies().get(2).getElements().get(0), convertedGridElement1);
    }

    void assertLfResult(ToOpLfResult lfResult, String contingencyId, String gridElementId, int side, double p, double loading) {
        ToOpCnecResult cnecResult = lfResult.getLfResult(gridElementId, contingencyId);
        if (cnecResult == null) {
            throw new IllegalStateException(MessageFormat.format("No result found for element {0} and contingency {1}", gridElementId, contingencyId));
        }

        assertThat(cnecResult.element()).isEqualTo(gridElementId);
        assertThat(cnecResult.contingency()).isEqualTo(contingencyId);
        assertThat(cnecResult.side()).isEqualTo(side);
        assertThat(cnecResult.p()).isCloseTo(p, Offset.offset(1e-3));
        assertThat(cnecResult.loading()).isCloseTo(loading, Offset.offset(1e-3));
    }

}
