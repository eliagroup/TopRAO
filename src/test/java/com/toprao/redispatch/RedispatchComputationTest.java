/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.redispatch;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.roda.parameters.RodaParameters;
import com.toprao.JsonUtils;
import com.toprao.crac.CracGenerationParameters;
import com.toprao.crac.RedispatchAction;
import com.toprao.redispatch.result.ActionSummary;
import com.toprao.redispatch.result.ActionType;
import com.toprao.redispatch.result.CnecSummary;
import com.toprao.redispatch.result.RaoSummary;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import com.toprao.utils.NetworkImportsUtil;
import org.apache.commons.io.FileUtils;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RedispatchComputationTest {

    public static final Offset<Double> OFFSET = Offset.offset(1e-3);

    public static final String PREVENTIVE_SCENARIO = "PreventiveScenario";

    private Network network;
    private ToOpN1Definition n1Definition;
    private ToOpLfResult lfResult;
    private RodaParameters forcedActions;
    private CracGenerationParameters cracGenerationParameters;

    @BeforeEach
    void setUp() {
        network = NetworkImportsUtil.import2NodesNetwork();
        n1Definition = JsonUtils.read(getClass().getResourceAsStream("/redispatch/2nodes/n1_definition_2nodes.json"), ToOpN1Definition.class);
        lfResult = JsonUtils.read(getClass().getResourceAsStream("/redispatch/2nodes/branch_results.json"), ToOpLfResult.class);
        forcedActions = new RodaParameters(List.of());
        cracGenerationParameters = new CracGenerationParameters();
    }

    @Test
    void runRedispatch2Nodes() {
        RaoSummary raoSummary = compute(lfResult);

        assertThat(raoSummary.isSecure()).isTrue();

        assertThat(raoSummary.getLimitingElements()).hasSize(4);
        assertLimitingElement(raoSummary.getLimitingElements().get(0), "France-Belgium interconnection n°2", "CO_France-Belgium interconnection n°1", 9.5234, "MW");
        assertLimitingElement(raoSummary.getLimitingElements().get(1), "France-Belgium interconnection n°1", "CO_France-Belgium interconnection n°2", 10.909, "MW");
        assertLimitingElement(raoSummary.getLimitingElements().get(2), "France-Belgium interconnection n°2", "BASECASE", 172.856, "MW");
        assertLimitingElement(raoSummary.getLimitingElements().get(3), "France-Belgium interconnection n°1", "BASECASE", 337.575, "MW");

        assertThat(raoSummary.getActions()).hasSize(2);
        assertAction(raoSummary.getActions().get(0), "RD_GEN_GENERATOR_BE_1.1_preventive", 310, 410);
        assertAction(raoSummary.getActions().get(1), "RD_GEN_GENERATOR_FR_1_preventive", -310, 410);
    }

    @Test
    void runRedispatch2NodesWithSA() {
        RaoSummary raoSummary = compute(null);

        assertThat(raoSummary.isSecure()).isTrue();

        assertThat(raoSummary.getLimitingElements()).hasSize(4);
        assertLimitingElement(raoSummary.getLimitingElements().get(0), "France-Belgium interconnection n°2", "CO_France-Belgium interconnection n°1", 9.5234, "MW");
        assertLimitingElement(raoSummary.getLimitingElements().get(1), "France-Belgium interconnection n°1", "CO_France-Belgium interconnection n°2", 10.909, "MW");
        assertLimitingElement(raoSummary.getLimitingElements().get(2), "France-Belgium interconnection n°2", "BASECASE", 172.856, "MW");
        assertLimitingElement(raoSummary.getLimitingElements().get(3), "France-Belgium interconnection n°1", "BASECASE", 337.575, "MW");

        assertThat(raoSummary.getActions()).hasSize(2);
        assertAction(raoSummary.getActions().get(0), "RD_GEN_GENERATOR_BE_1.1_preventive", 310, 410);
        assertAction(raoSummary.getActions().get(1), "RD_GEN_GENERATOR_FR_1_preventive", -310, 410);
    }

    @Test
    void testWithRedispatchCosts() {
        cracGenerationParameters.setRedispatchActions(List.of(
                new RedispatchAction("action_fr_1", "GENERATOR_FR_1", 2, 2, 2, 5000, 0),
                new RedispatchAction("action_be_1", "GENERATOR_BE_1.1", 4, 3, 3, 5000, 0)));

        RaoSummary raoSummary = compute(lfResult);

        assertThat(raoSummary.getActions()).hasSize(2);
        assertAction(raoSummary.getActions().get(0), "action_be_1", 310, 934);
        assertAction(raoSummary.getActions().get(1), "action_fr_1", -310, 622);
    }

    @Test
    void testWithMultiNodalRedispatchActions() {
        network.getGenerator("GENERATOR_BE_1.2").setTargetP(428.5);
        network.getGenerator("GENERATOR_FR_2").setTargetP(325);

        cracGenerationParameters.setRedispatchActions(List.of(
                new RedispatchAction("fr_1", Map.of("GENERATOR_FR_1", 0.8, "GENERATOR_FR_2", 0.2),
                        2, 2, 2, 5000, 0),
                new RedispatchAction("be_1", Map.of("GENERATOR_BE_1.1", 0.7, "GENERATOR_BE_1.2", 0.3),
                        4, 3, 3, 5000, 0)));

        RaoSummary raoSummary = compute(lfResult);

        assertThat(raoSummary.getActions()).hasSize(2);
        assertAction(raoSummary.getActions().get(0), "be_1", 292.666, 882);
        assertAction(raoSummary.getActions().get(1), "fr_1", -292, 586);

        network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
        assertThat(network.getGenerator("GENERATOR_BE_1.1").getTargetP()).isCloseTo(1204.7, OFFSET);
        assertThat(network.getGenerator("GENERATOR_BE_1.2").getTargetP()).isCloseTo(516.3, OFFSET);
        assertThat(network.getGenerator("GENERATOR_FR_1").getTargetP()).isCloseTo(1066.4, OFFSET);
        assertThat(network.getGenerator("GENERATOR_FR_2").getTargetP()).isCloseTo(266.6, OFFSET);
    }

    private RaoSummary compute(ToOpLfResult lfResults) {
        if (lfResults == null) {
            // No LF result input
            return RedispatchComputation.compute(network, n1Definition, forcedActions,
                    RaoParametersFactory.loadDefault(), cracGenerationParameters);
        }
        return RedispatchComputation.compute(network, n1Definition, lfResults, forcedActions,
                RaoParametersFactory.loadDefault(), cracGenerationParameters);
    }

    static void assertLimitingElement(CnecSummary cnec, String elementName, String contingencyName, double margin, String unit) {
        assertThat(cnec.elementName()).isEqualTo(elementName);
        assertThat(cnec.contingencyName()).isEqualTo(contingencyName);
        assertThat(cnec.margin()).isCloseTo(margin, OFFSET);
        assertThat(cnec.unit()).isEqualTo(unit);
    }

    static void assertAction(ActionSummary action, String name, double value, double cost) {
        assertThat(action.name()).isEqualTo(name);
        assertThat(action.value()).isCloseTo(value, OFFSET);
        assertThat(action.cost()).isCloseTo(cost, OFFSET);

    }

    @Test
    void writeRaoSummary(@TempDir Path outputDir) throws IOException {
        OffsetDateTime timestamp = OffsetDateTime.of(2026, 4, 25, 12, 0, 0, 0, ZoneOffset.UTC);
        List<ActionSummary> actions = List.of(new ActionSummary("PST_1_tap_change", "State_CURATIVE", timestamp, ActionType.PST, 10.4, 0.));
        List<CnecSummary> limitingElements = List.of(new CnecSummary("Branch 1", "CO Branch 2", 2.343, "A", "State_CURATIVE", timestamp));

        RaoSummary summary = new RaoSummary(true, 102.029, actions, limitingElements);
        Path summaryPath = outputDir.resolve("summary.json");
        JsonUtils.write(summaryPath, summary);
        String json = FileUtils.readFileToString(summaryPath.toFile(), StandardCharsets.UTF_8);
        assertThat(json).isEqualTo("""
              {
                "secure" : true,
                "functional_cost" : 102.03,
                "actions" : [ {
                  "name" : "PST_1_tap_change",
                  "state_id" : "State_CURATIVE",
                  "timestamp" : 1777118400.000000000,
                  "type" : "PST",
                  "value" : 10.40,
                  "cost" : 0.00
                } ],
                "limiting_elements" : [ {
                  "element_name" : "Branch 1",
                  "contingency_name" : "CO Branch 2",
                  "margin" : 2.34,
                  "unit" : "A",
                  "state_id" : "State_CURATIVE",
                  "timestamp" : 1777118400.000000000
                } ]
              }""");
    }

}
