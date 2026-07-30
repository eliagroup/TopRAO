/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.redispatch;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.toprao.crac.CracGenerationParameters;
import com.toprao.crac.FullPreventiveRaoSpecifier;
import com.toprao.toop.data.ToOpCnecResult;
import com.toprao.toop.data.ToOpContingency;
import com.toprao.toop.data.ToOpGridElement;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class CracGenerationTest {

    private Network network;

    ToOpGridElement branch1;
    ToOpContingency contingency1;
    ToOpContingency contingency2;
    private ToOpN1Definition n1Definition;
    private ToOpLfResult lfResult;
    private CracGenerationParameters parameters = new CracGenerationParameters();

    private void initFourBus() {
        network = TestNetworkFactory.createFourBusNetwork();

        branch1 = toGridElement(network.getLine("l14"));
        contingency1 = toContingency(network.getLine("l12"));
        contingency2 = toContingency(network.getLine("l13"));

        List<ToOpGridElement> monitoredElements = List.of(branch1);
        List<ToOpContingency> contingencies = List.of(contingency1, contingency2);

        n1Definition = new ToOpN1Definition(monitoredElements, contingencies, null);

        List<ToOpCnecResult> cnecResults = List.of(baseCaseResult(branch1, 0.8, 40),
                contingencyResult(branch1, contingency1, 0.92, 46),
                contingencyResult(branch1, contingency2, 0.98, 49));

        lfResult = new ToOpLfResult(cnecResults);
    }

    private void initPst() {
        network = TestNetworkFactory.createWithPst();

        branch1 = toGridElement(network.getLine("L2"));
        contingency1 = toContingency(network.getLine("L1"));

        List<ToOpGridElement> monitoredElements = List.of(branch1);
        List<ToOpContingency> contingencies = List.of(contingency1);

        n1Definition = new ToOpN1Definition(monitoredElements, contingencies, null);

        List<ToOpCnecResult> cnecResults = List.of(baseCaseResult(branch1, 0.8, 40),
                contingencyResult(branch1, contingency1, 0.92, 46));

        lfResult = new ToOpLfResult(cnecResults);

        parameters.setRedispatchActions(false);
    }

    @Test
    void defaultParameters() {
        initFourBus();
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);

        // Preventive cnec is kept
        Cnec<?> basCaseCnec = crac.getCnec("l14_preventive");
        // cnec l14 + CO l12 kept: diff = 0.12 (>0.1) and p diff = 6 (>5)
        Cnec<?> outageCnec1 = crac.getCnec("l14_CO_l12_outage");
        // cnec l14 + CO l13 kept: loading diff = 0.18 (>0.1) and p diff = 9 (>5)
        Cnec<?> outageCnec2 = crac.getCnec("l14_CO_l13_outage");
        assertThat(crac.getCnecs()).containsExactlyInAnyOrder(outageCnec1, outageCnec2, basCaseCnec);

        assertThat(basCaseCnec.getNetworkElements()).hasSize(1);
        assertThat(basCaseCnec.getNetworkElements().stream().findFirst().get().getId()).isEqualTo(branch1.getId());
        assertThat(basCaseCnec.getState().getContingency()).isEmpty();

        assertThat(outageCnec1.getNetworkElements()).hasSize(1);
        assertThat(outageCnec1.getNetworkElements().stream().findFirst().get().getId()).isEqualTo(branch1.getId());
        assertThat(outageCnec1.getState().getContingency().get().getId()).isEqualTo(contingency1.getId());

        assertThat(outageCnec2.getNetworkElements()).hasSize(1);
        assertThat(outageCnec2.getNetworkElements().stream().findFirst().get().getId()).isEqualTo(branch1.getId());
        assertThat(outageCnec2.getState().getContingency().get().getId()).isEqualTo(contingency2.getId());
    }

    @Test
    void allContingencyCnecsFiltered() {
        initFourBus();
        parameters.setAffectedCnecMinActivePowerDiff(10);
        parameters.setAffectedCnecMinLoadingDiff(0.3);

        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);

        assertThat(crac.getCnecs()).containsExactlyInAnyOrder(crac.getCnec("l14_preventive"));
    }

    @Test
    void cnecFilteredByLoading() {
        initFourBus();
        parameters.setAffectedCnecMinActivePowerDiff(0);
        parameters.setAffectedCnecMinLoadingDiff(0.13);

        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);

        // cnec l14 + CO l12 filtered : p diff = 6 (>0) BUT loading diff = 0.12 (<0.13)
        assertThat(crac.getCnecs()).containsExactlyInAnyOrder(
                crac.getCnec("l14_CO_l13_outage"),
                crac.getCnec("l14_preventive"));
    }

    @Test
    void cnecFilteredByActivePower() {
        initFourBus();
        parameters.setAffectedCnecMinActivePowerDiff(7);
        parameters.setAffectedCnecMinLoadingDiff(0);

        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);

        // cnec l14 + CO l12 filtered : loading diff = 0.08 (>0) BUT p diff = 6 (<7)
        assertThat(crac.getCnecs()).containsExactlyInAnyOrder(
                crac.getCnec("l14_CO_l13_outage"),
                crac.getCnec("l14_preventive"));
    }

    @Test
    void keepBranchHasRequiredDiffOnOneSide() {
        initFourBus();
        List<ToOpCnecResult> cnecResults = List.of(baseCaseResult(branch1, 1, 0.8, 40),
                baseCaseResult(branch1, 2, 0.8, 40),
                contingencyResult(branch1, contingency2, 1, 0.8, 40),
                contingencyResult(branch1, contingency2, 2, 0.98, 49));
        lfResult = new ToOpLfResult(cnecResults);
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getCnecs()).containsExactlyInAnyOrder(
                crac.getCnec("l14_CO_l13_outage"),
                crac.getCnec("l14_preventive"));

        // swap sides
        cnecResults = List.of(baseCaseResult(branch1, 0.8, 40),
                baseCaseResult(branch1, 2, 0.8, 40),
                contingencyResult(branch1, contingency2, 2, 0.8, 40),
                contingencyResult(branch1, contingency2, 1, 0.98, 49));
        lfResult = new ToOpLfResult(cnecResults);
        FullPreventiveRaoSpecifier raoSpecifier2 = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac2 = RaoRunner.generateCrac(raoSpecifier2, network);
        assertThat(crac2.getCnecs()).containsExactlyInAnyOrder(
                crac2.getCnec("l14_CO_l13_outage"),
                crac2.getCnec("l14_preventive"));
    }

    @Test
    void monitoredBranchIgnoredWhenNoLimits() {
        initFourBus();
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup1().removeActivePowerLimits();
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup2().removeActivePowerLimits();
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getCnecs()).hasSize(0);
    }

    @Test
    void monitoredBranchOneSideLimitAccepted() {
        initFourBus();
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup1().removeActivePowerLimits();
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getCnecs()).hasSize(3);
    }

    @Test
    void monitoredBranchCurrentLimitsAccepted() {
        initFourBus();
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup1().removeActivePowerLimits();
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup1().newCurrentLimits().setPermanentLimit(2).add();

        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getCnecs()).hasSize(3);
    }

    @Test
    void generatorRemedialActions() {
        initFourBus();
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);

        RangeAction<?> actionG1 = crac.getRangeAction("RD_GEN_g1_preventive");
        RangeAction<?> actionG4 = crac.getRangeAction("RD_GEN_g4_preventive");
        assertThat(crac.getRangeActions()).containsExactlyInAnyOrder(actionG1, actionG4);

        assertThat(actionG1.getNetworkElements()).hasSize(1);
        assertThat(actionG1.getNetworkElements().stream().findFirst().get().getId()).isEqualTo("g1");
        assertThat(actionG1.getMaxAdmissibleSetpoint(30)).isEqualTo(50);
        assertThat(actionG1.getMinAdmissibleSetpoint(30)).isEqualTo(10);

        assertThat(actionG4.getNetworkElements()).hasSize(1);
        assertThat(actionG4.getNetworkElements().stream().findFirst().get().getId()).isEqualTo("g4");
        assertThat(actionG4.getMaxAdmissibleSetpoint(30)).isEqualTo(60);
        assertThat(actionG4.getMinAdmissibleSetpoint(30)).isEqualTo(20);
    }

    @Test
    void generatorRemedialActionsDeactivated() {
        initFourBus();
        parameters.setRedispatchActions(false);
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getRangeActions()).isEmpty();
    }

    @Test
    void generatorRemedialActionsSetPointsFixed() {
        initFourBus();
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);

        InjectionRangeAction actionG1 = crac.getInjectionRangeAction("RD_GEN_g1_preventive");

        // min/max of remedial action are fixed (do not depend on previous instant setpoint)
        assertThat(actionG1.getMaxAdmissibleSetpoint(5)).isEqualTo(50);
        assertThat(actionG1.getMaxAdmissibleSetpoint(10)).isEqualTo(50);
        assertThat(actionG1.getMaxAdmissibleSetpoint(42)).isEqualTo(50);
        assertThat(actionG1.getMaxAdmissibleSetpoint(70)).isEqualTo(50);

        assertThat(actionG1.getMinAdmissibleSetpoint(5)).isEqualTo(10);
        assertThat(actionG1.getMinAdmissibleSetpoint(10)).isEqualTo(10);
        assertThat(actionG1.getMinAdmissibleSetpoint(42)).isEqualTo(10);
        assertThat(actionG1.getMinAdmissibleSetpoint(70)).isEqualTo(10);
    }

    @Test
    void generatorRemedialActionsDefaultCosts() {
        initFourBus();
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);

        InjectionRangeAction actionG1 = crac.getInjectionRangeAction("RD_GEN_g1_preventive");

        assertThat(actionG1.getActivationCost().get()).isEqualTo(100);
        assertThat(actionG1.getVariationCost(VariationDirection.UP).get()).isEqualTo(1);
        assertThat(actionG1.getVariationCost(VariationDirection.DOWN).get()).isEqualTo(1);
    }

    @Test
    void generatorRemedialActionsCosts() {
        initFourBus();
        parameters.setRedispatchCostUp(2);
        parameters.setRedispatchCostDown(3);
        parameters.setRedispatchActivationCost(33);

        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);

        InjectionRangeAction actionG1 = crac.getInjectionRangeAction("RD_GEN_g1_preventive");

        assertThat(actionG1.getActivationCost().get()).isEqualTo(33);
        assertThat(actionG1.getVariationCost(VariationDirection.UP).get()).isEqualTo(2);
        assertThat(actionG1.getVariationCost(VariationDirection.DOWN).get()).isEqualTo(3);
    }

    @Test
    void generatorRemedialActionsRequestedMaxP() {
        initFourBus();
        parameters.setRedispatchGeneratorRequiredMaxP(65);
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getRangeActions()).isEmpty();

        parameters.setRedispatchGeneratorRequiredMaxP(55);
        raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getRangeActions()).hasSize(1);

        parameters.setRedispatchGeneratorRequiredMaxP(45);
        raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getRangeActions()).hasSize(2);
    }

    @Test
    void generatorRemedialActionsCountries() {
        initFourBus();
        // TODO add same test for psts
        parameters.setRangeActionsCountries(Set.of(Country.FR));
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getRangeActions()).hasSize(2);

        parameters.setRangeActionsCountries(Set.of(Country.BE));
        raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getRangeActions()).isEmpty();
    }

    @Test
    void pstRemedialActions() {
        initPst();
        parameters.setRedispatchActions(false);
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);

        PstRangeAction actionPst1 = crac.getPstRangeAction("PST_RA_PS1_preventive");
        assertThat(crac.getRangeActions()).containsExactlyInAnyOrder(actionPst1);

        assertThat(actionPst1.getNetworkElements()).hasSize(1);
        assertThat(actionPst1.getNetworkElements().stream().findFirst().get().getId()).isEqualTo("PS1");

        assertThat(actionPst1.getMaxAdmissibleSetpoint(-4.5)).isEqualTo(5);
        assertThat(actionPst1.getMaxAdmissibleSetpoint(0)).isEqualTo(5);
        assertThat(actionPst1.getMaxAdmissibleSetpoint(3)).isEqualTo(5);

        assertThat(actionPst1.getMinAdmissibleSetpoint(-4.5)).isEqualTo(-5);
        assertThat(actionPst1.getMinAdmissibleSetpoint(0)).isEqualTo(-5);
        assertThat(actionPst1.getMinAdmissibleSetpoint(3)).isEqualTo(-5);
    }

    @Test
    void pstRemedialActionsDisabled() {
        initPst();
        parameters.setPstActions(false);
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getRangeActions()).isEmpty();
    }

    // todo test pst actions countries filter
    // todo test pst as monitored element + contingency + one of those and RA

    ToOpGridElement toGridElement(Branch<?> branch) {
        return new ToOpGridElement(branch.getId(), branch.getNameOrId(), "branch", branch.getType().name());
    }

    ToOpContingency toContingency(Branch<?> branch) {
        return new ToOpContingency("CO_" + branch.getId(), "CO " + branch.getNameOrId(), List.of(toGridElement(branch)));
    }

    ToOpCnecResult baseCaseResult(ToOpGridElement gridElement, double loading, double p) {
        return baseCaseResult(gridElement, 1, loading, p);
    }

    ToOpCnecResult baseCaseResult(ToOpGridElement gridElement, int side, double loading, double p) {
        return new ToOpCnecResult(gridElement.getId(), "BASECASE", side, loading, p);
    }

    ToOpCnecResult contingencyResult(ToOpGridElement gridElement, ToOpContingency contingency, double loading, double p) {
        return contingencyResult(gridElement, contingency, 1, loading, p);
    }

    ToOpCnecResult contingencyResult(ToOpGridElement gridElement, ToOpContingency contingency, int side, double loading, double p) {
        return new ToOpCnecResult(gridElement.getId(), contingency.getId(), side, loading, p);
    }

}
