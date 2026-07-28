/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.redispatch;

import com.powsybl.action.Action;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.roda.parameters.RodaParameters;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.impl.FastRaoResultImpl;
import com.powsybl.security.PostContingencyComputationStatus;
import com.powsybl.security.results.PostContingencyResult;
import com.toprao.crac.CracCreationSpecifier;
import com.toprao.crac.CracGenerationParameters;
import com.toprao.crac.FullPreventiveRaoSpecifier;
import com.toprao.redispatch.result.ActionSummary;
import com.toprao.redispatch.result.ActionType;
import com.toprao.redispatch.result.CnecSummary;
import com.toprao.redispatch.result.RaoSummary;
import com.toprao.sa.SaResultsConverter;
import com.toprao.sa.SecurityAnalysisRunner;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public final class RedispatchComputation {

    private RedispatchComputation() {
    }

    public static final int CPUS_COUNT = 1;

    public static RaoSummary compute(@NonNull Network network,
                               @NonNull ToOpN1Definition n1Definition,
                               @NonNull RodaParameters forcedActions,
                               @NonNull RaoParameters raoParameters,
                               @NonNull CracGenerationParameters cracGenerationParameters) {

        // Run SA to generate ToOpLfResult
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        String saVariantId = "SA_run";
        network.getVariantManager().cloneVariant(initialVariantId, saVariantId, true);
        network.getVariantManager().setWorkingVariant(saVariantId);

        List<Action> networkActions = forcedActions.getForcedPreventiveActions();
        if (!networkActions.isEmpty()) {
            networkActions.forEach(na -> na.toModification().apply(network));
        }
        ToOpLfResult toOpLfResult = runSa(network, n1Definition);
        network.getVariantManager().setWorkingVariant(initialVariantId);
        network.getVariantManager().removeVariant(saVariantId);

        return compute(network, n1Definition, toOpLfResult, forcedActions, raoParameters, cracGenerationParameters);
    }

    public static RaoSummary compute(@NonNull Network network,
                               @NonNull ToOpN1Definition n1Definition,
                               @NonNull ToOpLfResult lfResult,
                               @NonNull RodaParameters forcedActions,
                               @NonNull RaoParameters raoParameters,
                               @NonNull CracGenerationParameters cracGenerationParameters) {

        addRodaParameters(raoParameters, forcedActions);

        CracCreationSpecifier cracCreationSpecifier = new FullPreventiveRaoSpecifier(n1Definition, lfResult, cracGenerationParameters);
        RaoRunner raoRunner = new RaoRunner();
        TimeCoupledRaoResult raoResult = raoRunner.run(network, cracCreationSpecifier, raoParameters);

        return createRaoSummary(raoResult, raoRunner.getTimeRaoInput());

    }

    private static RaoSummary createRaoSummary(TimeCoupledRaoResult result, TimeCoupledRaoInput timeCoupledRaoInput) {
        OffsetDateTime dt = timeCoupledRaoInput.getTimestampsToRun().stream().toList().getFirst();
        Crac crac = timeCoupledRaoInput.getRaoInputs().getData(dt).get().getCrac();

        double totalCost = result.getFunctionalCost(crac.getLastInstant(), dt);

        // TODO take actions from all states (ok like this for now, actions are only preventive)
        List<ActionSummary> actionSummaries = crac.getStates(crac.getInstant("preventive")).stream()
            .flatMap(state -> result.getActivatedRangeActionsDuringState(state)
            .stream().map(a -> toActionSummary(a, result, state)))
            .sorted(Comparator.comparingDouble(ActionSummary::cost).reversed().thenComparing(ActionSummary::name))
            .toList();

        FastRaoResultImpl timestampResult = (FastRaoResultImpl) result.getIndividualRaoResult(dt);

        FlowResult flowResult = timestampResult.getFinalResult();
        List<FlowCnec> mostCriticalElementsPreventive = timestampResult.getMostLimitingElements(crac.getInstant("preventive"), 6);
        List<FlowCnec> mostCriticalElementsOutages = timestampResult.getMostLimitingElements(crac.getInstant("outage"), 6);
        List<CnecSummary> limitingElements = Stream.of(mostCriticalElementsPreventive, mostCriticalElementsOutages)
                .flatMap(List::stream)
                .map(e -> toCnecSummary(flowResult, e))
                .sorted(Comparator.comparingDouble(CnecSummary::margin))
                .distinct()
                .limit(6)
                .toList();

        boolean isSecure = limitingElements.stream().noneMatch(e -> e.margin() <= 0);
        return new RaoSummary(isSecure, totalCost, actionSummaries, limitingElements);
    }

    private static CnecSummary toCnecSummary(FlowResult flowResult, FlowCnec cnec) {
        double margin = flowResult.getMargin(cnec, Unit.MEGAWATT);
        cnec.getNetworkElement();
        String contingencyName = getContingencyNameOrId(cnec);
        return new CnecSummary(cnec.getNetworkElement().getName(), contingencyName, margin, Unit.MEGAWATT.toString(), cnec.getState().getId(), cnec.getState().getTimestamp().orElse(null));
    }

    private static String getContingencyNameOrId(FlowCnec cnec) {
        Optional<Contingency> contingencyOpt = cnec.getState().getContingency();
        if (contingencyOpt.isPresent()) {
            if (contingencyOpt.get().getName().isPresent()) {
                return contingencyOpt.get().getName().get();
            } else {
                return contingencyOpt.get().getId();
            }
        }
        return "BASECASE";
    }

    private static ActionSummary toActionSummary(RangeAction<?> rangeAction, TimeCoupledRaoResult raoResult, State state) {
        double before;
        double after;
        ActionType actionType;

        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            before = raoResult.getPreOptimizationTapOnState(state, pstRangeAction);
            after = raoResult.getOptimizedTapOnState(state, pstRangeAction);
            actionType = ActionType.PST;
        } else {
            before = raoResult.getPreOptimizationSetPointOnState(state, rangeAction);
            after = raoResult.getOptimizedSetPointOnState(state, rangeAction);
            actionType = ActionType.GENERATOR;
        }
        double variation = after - before;

        if (Math.abs(variation) < 1e-6) {
            variation = 0.;
        }

        return new ActionSummary(
                rangeAction.getName(),
                state.getId(),
                state.getTimestamp().orElse(null),
                actionType,
                variation,
                rangeAction.getTotalCostForVariation(variation));
    }

    private static void addRodaParameters(RaoParameters raoParameters, RodaParameters forcedActions) {
        RodaParameters parametersExt = raoParameters.getExtension(RodaParameters.class);
        if (parametersExt != null) {
            raoParameters.removeExtension(RodaParameters.class);
        }
        raoParameters.addExtension(RodaParameters.class, forcedActions);
    }

    private static ToOpLfResult runSa(Network network, ToOpN1Definition n1Definition) {
        log.info("N-1 LF results not provided, running security analysis on N-1 definition");
        var saResult = new SecurityAnalysisRunner().run(network, n1Definition, CPUS_COUNT);
        List<PostContingencyResult> nonConvergedResults = saResult.getPostContingencyResults().stream().filter(r -> r.getStatus() != PostContingencyComputationStatus.CONVERGED).toList();
        log.info("Security analysis run finished with {} non converged contingencies", nonConvergedResults.size());
        return new SaResultsConverter().convertResults(saResult, network);
    }
}
