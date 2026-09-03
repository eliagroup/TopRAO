package com.toprao.crac;

import com.powsybl.contingency.ContingencyElement;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.io.network.NetworkCracCreator;
import com.powsybl.openrao.data.crac.io.network.parameters.CriticalElements;
import com.powsybl.openrao.data.crac.io.network.parameters.NetworkCracCreationParameters;
import com.powsybl.openrao.data.crac.io.network.parameters.PstRangeActions;
import com.powsybl.openrao.data.crac.io.network.parameters.RangeActionCosts;
import com.toprao.toop.data.ToOpCnecResult;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class TopRaoCracCreator implements CracCreator {

    private static final String PREVENTIVE = "preventive";
    private static final String OUTAGE = "outage";
    private static final String BASE_CASE_ID = "BASECASE";
    private static final Double CRITICAL_ELEMENTS_OPTIMIZED_MIN_V = null;

    private CracGenerationParameters cracGenerationParameters;
    private ToOpN1Definition n1Definition;
    private ToOpLfResult toOpLfResult;

    @Override
    public Crac generateCrac(Network network) {
        CracCreationParameters cracCreationParameters = getCracCreationParameters();
        CracCreationContext ccc = NetworkCracCreator.createCrac(network, cracCreationParameters);
        ccc.getCreationReport().printCreationReport();

        Crac crac = ccc.getCrac();
        addRedispatchActions(crac);
        return crac;
    }

private void addRedispatchActions(Crac crac) {
    if (!cracGenerationParameters.isRedispatchActionsActive() || cracGenerationParameters.getRedispatchActions().isEmpty()) {
        return;
    }

    cracGenerationParameters.getRedispatchActions().stream()
            .filter(a -> a.getGeneratorId() != null)
            .filter(a -> Double.isFinite(a.getActivationCost())
                    && Double.isFinite(a.getVariationCostUp())
                    && Double.isFinite(a.getVariationCostDown())
                    && Double.isFinite(a.getActivePowerMax())
                    && Double.isFinite(a.getActivePowerMin()))
            .forEach(a -> addRedispatchAction(a, crac));
}

    private void addRedispatchAction(RedispatchAction action, Crac crac) {
        crac.newInjectionRangeAction()
                .withId("RD_GEN_%s_%s".formatted(action.getGeneratorId(), PREVENTIVE))
                .withNetworkElement(action.getGeneratorId())
                .withActivationCost(action.getActivationCost())
                .withVariationCost(action.getVariationCostUp(), VariationDirection.UP)
                .withVariationCost(action.getVariationCostDown(), VariationDirection.DOWN)
                .newOnInstantUsageRule()
                    .withInstant(PREVENTIVE)
                    .add()
                .newRange()
                    .withMax(action.getActivePowerMax())
                    .withMin(action.getActivePowerMin())
                    .withRangeType(RangeType.ABSOLUTE)
                    .add()
                .add();
    }

    private CracCreationParameters getCracCreationParameters() {
        log.info("Creating Crac creation parameters");
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        NetworkCracCreationParameters parameters = new NetworkCracCreationParameters(null, List.of());
        cracCreationParameters.addExtension(NetworkCracCreationParameters.class, parameters);

        createCnecParameters(parameters);
        createRaParameters(parameters);

        log.info("Crac creation parameters generated");
        return cracCreationParameters;
    }

    private void createCnecParameters(NetworkCracCreationParameters parameters) {
        createFilteredCnecParameters(parameters);
        parameters.getCriticalElements().setThresholdDefinition(CriticalElements.ThresholdDefinition.PERM_LIMIT_MULTIPLIER);
        parameters.getCriticalElements().setLimitMultiplierPerInstant(Map.of(PREVENTIVE, cracGenerationParameters.getLimitMultiplierPreventive(), OUTAGE, cracGenerationParameters.getLimitMultiplierOutage()));
    }

    protected void createRaParameters(NetworkCracCreationParameters parameters) {
        parameters.getCountertradingRangeActions().setCountryFilter(Set.of());

        if (cracGenerationParameters.isPstActionsActive()) {
            parameters.getPstRangeActions().setCountryFilter(cracGenerationParameters.getRangeActionsCountries());
            parameters.getPstRangeActions().setPstRaPredicate((pst, state, context) -> state.isPreventive());
            parameters.getPstRangeActions().setAvailableTapRangesAtInstants(
                    Map.of(
                            PREVENTIVE, new PstRangeActions.TapRange(
                                    -cracGenerationParameters.getPstTapChangeMaxDown(),
                                    cracGenerationParameters.getPstTapChangeMaxUp(),
                                    RangeType.RELATIVE_TO_INITIAL_NETWORK))
            );
        } else {
            parameters.getPstRangeActions().setCountryFilter(Set.of());
        }

        if (cracGenerationParameters.isRedispatchActionsActive() && cracGenerationParameters.getRedispatchActions().isEmpty()) {
            parameters.getRedispatchingRangeActions().setIncludeAllInjections(true);
            parameters.getRedispatchingRangeActions().setCountryFilter(cracGenerationParameters.getRangeActionsCountries());

            parameters.getRedispatchingRangeActions().setRdRaPredicate((injection, instant, context) -> instant.isPreventive()
                    && injection.getType() == IdentifiableType.GENERATOR
                    && ((Generator) injection).getMaxP() > cracGenerationParameters.getRedispatchGeneratorRequiredMaxP()
                    && ((Generator) injection).getMaxP() < Double.MAX_VALUE
                    && ((Generator) injection).getMinP() > -Double.MAX_VALUE);
            parameters.getRedispatchingRangeActions().setRaCostsProvider((injection, instant) ->
                    new RangeActionCosts(cracGenerationParameters.getRedispatchActivationCost(), cracGenerationParameters.getRedispatchCostUp(), cracGenerationParameters.getRedispatchCostDown()));
        } else {
            parameters.getRedispatchingRangeActions().setIncludeAllInjections(false);
            parameters.getRedispatchingRangeActions().setCountryFilter(Set.of());
        }
    }

    protected void createFilteredCnecParameters(NetworkCracCreationParameters parameters) {
        // TODO add a warning if the branch does not have limits on both sides
        List<String> monitoredBranchesIdsBaseCase = n1Definition.getMonitoredElements().stream().map(e -> e.getId()).toList();

        // TODO check that each cnec of n1 definition has a loadflow result associated
        Map<String, List<ToOpCnecResult>> selectedResultsByContingency = selectCnecs();
        List<String> selectedContingencyElementIds = selectedResultsByContingency.keySet().stream().flatMap(c -> n1Definition.getContingency(c).getElements().stream()).map(e -> e.getId()).toList();

        log.info("Contingency elements count : {}", selectedContingencyElementIds.size());
        parameters.getContingencies().setBranchFilter(b -> selectedContingencyElementIds.contains(b.getId()));

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
        List<String> n1DefContingencyIds = n1Definition.getContingencies().stream().map(c -> c.getId()).toList();
        List<String> monitoredElements = n1Definition.getMonitoredElements().stream().map(e -> e.getId()).toList();

        Map<String, List<ToOpCnecResult>> baseCaseResults = toOpLfResult.getLfResultsForContingency(BASE_CASE_ID)
                .stream()
                .collect(Collectors.groupingBy(r -> r.element()));

        List<ToOpCnecResult> resultsOk = toOpLfResult.getResults()
                .stream()
                .filter(r -> n1DefContingencyIds.contains(r.contingency()))
                .filter(r -> monitoredElements.contains(r.element()))
                .filter(r -> isAffectedByContingency(r, baseCaseResults))
                .toList();
        return resultsOk.stream()
                .collect(Collectors.groupingBy(r -> r.contingency()));
    }

    private boolean isAffectedByContingency(ToOpCnecResult r, Map<String, List<ToOpCnecResult>> baseCaseResults) {
        List<ToOpCnecResult> elementBaseCaseResults = baseCaseResults.getOrDefault(r.element(), List.of());
        Optional<ToOpCnecResult> baseCaseRes = elementBaseCaseResults.stream()
                .filter(bcr -> bcr.side() == r.side())
                .findFirst();
        if (baseCaseRes.isEmpty()) {
            log.warn("No base case result for {}", r.element());
            return false;
        }
        return Math.abs(baseCaseRes.get().loading() - r.loading()) > cracGenerationParameters.getAffectedCnecMinLoadingDiff()
                && Math.abs(baseCaseRes.get().p() - r.p()) > cracGenerationParameters.getAffectedCnecMinActivePowerDiff();
    }
}
