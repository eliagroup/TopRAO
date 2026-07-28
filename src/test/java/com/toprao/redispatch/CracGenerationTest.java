package com.toprao.redispatch;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.toprao.crac.CracGenerationParameters;
import com.toprao.crac.FullPreventiveRaoSpecifier;
import com.toprao.toop.data.ToOpCnecResult;
import com.toprao.toop.data.ToOpContingency;
import com.toprao.toop.data.ToOpGridElement;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class CracGenerationTest {

    private Network network;

    ToOpGridElement l14;
    ToOpContingency coL12;
    ToOpContingency coL13;
    private ToOpN1Definition n1Definition;
    private ToOpLfResult lfResult;
    private CracGenerationParameters parameters = new CracGenerationParameters();

    @BeforeEach
    void setup() {
        network = FourBusRaoNetworkFactory.createBaseNetwork();

        l14 = toGridElement(network.getLine("l14"));
        coL12 = toContingency(network.getLine("l12"));
        coL13 = toContingency(network.getLine("l13"));

        List<ToOpGridElement> monitoredElements = List.of(l14);
        List<ToOpContingency> contingencies = List.of(coL12, coL13);

        n1Definition = new ToOpN1Definition(monitoredElements, contingencies, null);

        List<ToOpCnecResult> cnecResults = List.of(baseCaseResult(l14, 0.8, 40),
                contingencyResult(l14, coL12, 0.92, 46),
                contingencyResult(l14, coL13, 0.98, 49));

        lfResult = new ToOpLfResult(cnecResults);
    }

    @Test
    void defaultParameters() {
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
        assertThat(basCaseCnec.getNetworkElements().stream().findFirst().get().getId()).isEqualTo(l14.getId());
        assertThat(basCaseCnec.getState().getContingency()).isEmpty();

        assertThat(outageCnec1.getNetworkElements()).hasSize(1);
        assertThat(outageCnec1.getNetworkElements().stream().findFirst().get().getId()).isEqualTo(l14.getId());
        assertThat(outageCnec1.getState().getContingency().get().getId()).isEqualTo(coL12.getId());

        assertThat(outageCnec2.getNetworkElements()).hasSize(1);
        assertThat(outageCnec2.getNetworkElements().stream().findFirst().get().getId()).isEqualTo(l14.getId());
        assertThat(outageCnec2.getState().getContingency().get().getId()).isEqualTo(coL13.getId());
    }

    @Test
    void allContingencyCnecsFiltered() {
        parameters.setAffectedCnecMinActivePowerDiff(10);
        parameters.setAffectedCnecMinLoadingDiff(0.3);

        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);

        assertThat(crac.getCnecs()).containsExactlyInAnyOrder(crac.getCnec("l14_preventive"));
    }

    @Test
    void cnecFilteredByLoading() {
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
    void monitoredBranchIgnoredWhenNoLimits() {
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup1().removeActivePowerLimits();
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup2().removeActivePowerLimits();
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getCnecs()).hasSize(0);
    }

    @Test
    void monitoredBranchOneSideLimitAccepted() {
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup1().removeActivePowerLimits();
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getCnecs()).hasSize(3);
    }

    @Test
    void monitoredBranchCurrentLimitsAccepted() {
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup1().removeActivePowerLimits();
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup1().newCurrentLimits().setPermanentLimit(2).add();

        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);
        assertThat(crac.getCnecs()).hasSize(3);
    }

    @Test
    void generatorRemedialActions() {
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
        parameters.setRedispatchActions(false);

        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);

        assertThat(crac.getRangeActions()).isEmpty();

    }

    @Test
    void generatorRemedialActionsSetPointsFixed() {
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
        FullPreventiveRaoSpecifier raoSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, parameters);
        Crac crac = RaoRunner.generateCrac(raoSpecifier, network);

        InjectionRangeAction actionG1 = crac.getInjectionRangeAction("RD_GEN_g1_preventive");

        assertThat(actionG1.getActivationCost().get()).isEqualTo(100);
        assertThat(actionG1.getVariationCost(VariationDirection.UP).get()).isEqualTo(1);
        assertThat(actionG1.getVariationCost(VariationDirection.DOWN).get()).isEqualTo(1);
    }

    @Test
    void generatorRemedialActionsCosts() {
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

    ToOpGridElement toGridElement(Branch<?> branch) {
        return new ToOpGridElement(branch.getId(), branch.getNameOrId(), "branch", branch.getType().name());
    }

    ToOpContingency toContingency(Branch<?> branch) {
        return new ToOpContingency("CO_" + branch.getId(), "CO " + branch.getNameOrId(), List.of(toGridElement(branch)));
    }

    ToOpCnecResult baseCaseResult(ToOpGridElement gridElement, double loading, double p) {
        return new ToOpCnecResult(gridElement.getId(), "BASECASE", 1, loading, p);
    }

    ToOpCnecResult contingencyResult(ToOpGridElement gridElement, ToOpContingency contingency, double loading, double p) {
        return new ToOpCnecResult(gridElement.getId(), contingency.getId(), 1, loading, p);
    }

}
