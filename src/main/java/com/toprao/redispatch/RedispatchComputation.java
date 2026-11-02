/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.redispatch;

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
import com.toprao.crac.CracCreator;
import com.toprao.redispatch.result.ActionSummary;
import com.toprao.redispatch.result.ActionType;
import com.toprao.redispatch.result.CnecSummary;
import com.toprao.redispatch.result.RaoSummary;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RedispatchComputation {

    Network network;
    RodaParameters forcedActions;
    RaoParameters raoParameters;
    CracCreator cracCreator;

    public RaoSummary compute() {
        log.info("Generating Crac");
        Crac crac = cracCreator.create(network);
        log.info("Crac generated");
        log.info("Nb of CNECs : {}", crac.getCnecs().size());
        log.info("Nb of preventive CNECs: {}", crac.getCnecs().stream().filter(c -> c.getState().isPreventive()).count());
        log.info("Nb of curative CNECs : {}", crac.getCnecs().stream().filter(c -> !c.getState().isPreventive()).count());
        log.info("Nb of injection actions : {}", crac.getInjectionRangeActions().size());
        log.info("Nb of pst actions : {}", crac.getPstRangeActions().size());

        addRodaParameters(raoParameters, forcedActions);
        RaoRunner raoRunner = new RaoRunner();
        TimeCoupledRaoResult raoResult = raoRunner.run(network, crac, raoParameters);
        return createRaoSummary(raoResult, raoRunner.getTimeRaoInput());

    }

    private static RaoSummary createRaoSummary(TimeCoupledRaoResult result, TimeCoupledRaoInput timeCoupledRaoInput) {
        OffsetDateTime dt = timeCoupledRaoInput.getTimestampsToRun().stream().toList().getFirst();
        Crac crac = timeCoupledRaoInput.getRaoInputs().getData(dt)
            .orElseThrow(() -> new IllegalStateException("Missing RAO input for timestamp " + dt))
            .getCrac();

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
        return contingencyOpt.map(contingency -> contingency.getName().orElse(contingency.getId()))
                .orElse("BASECASE");
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
}
