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
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.roda.parameters.RodaParameters;
import com.toprao.crac.CracGenerationParameters;
import com.toprao.redispatch.result.ActionSummary;
import com.toprao.redispatch.result.ActionType;
import com.toprao.redispatch.result.CnecSummary;
import com.toprao.redispatch.result.RaoSummary;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import com.toprao.utils.NetworkImportsUtil;
import org.apache.commons.io.FileUtils;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RedispatchComputationTest {

    public static final Offset<Double> OFFSET = Offset.offset(1e-3);

    @Test
    void runRedispatch2Nodes() {
        Network network = NetworkImportsUtil.import2NodesNetwork();
        ToOpN1Definition n1Definition = ToOpN1Definition.readFromInputStream(getClass().getResourceAsStream("/redispatch/2nodes/n1_definition_2nodes.json"));
        ToOpLfResult lfResult = ToOpLfResult.readFromInputStream(getClass().getResourceAsStream("/redispatch/2nodes/branch_results.json"));
        RaoParameters raoParameters = RaoParametersFactory.loadDefault();
        RodaParameters forcedActions = new RodaParameters(List.of());
        CracGenerationParameters cracGenerationParameters = new CracGenerationParameters();

        RaoSummary raoSummary = RedispatchComputation.compute(network, n1Definition, lfResult, forcedActions, raoParameters, cracGenerationParameters);

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
        summary.write(summaryPath);
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
