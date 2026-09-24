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
import com.powsybl.action.SwitchAction;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.roda.parameters.RodaParameters;
import com.toprao.cli.InputFilesReader;
import com.toprao.crac.CracGenerationParameters;
import com.toprao.crac.RedispatchAction;
import com.toprao.toop.data.ToOpCnecResult;
import com.toprao.toop.data.ToOpContingency;
import com.toprao.toop.data.ToOpGridElement;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import com.toprao.utils.TestAssertUtils;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class InputsReaderTest {

    @Test
    void readForcedActions() {
        String inputJson = """
                {
                  "forced-actions": {
                    "preventive-actions-list": {
                      "version": 1.3,
                      "actions": [
                        {
                          "type": "SWITCH",
                          "id": "Open Switch1",
                          "switchId": "Switch1",
                          "open": true
                        },
                        {
                          "type": "SWITCH",
                          "id": "Close Switch2",
                          "switchId": "Switch2",
                          "open": false
                        }
                      ]
                    }
                  }
                }
                """;

        RodaParameters rodaParameters = InputFilesReader.readTopologicalAction(inputJson);

        List<Action> actions = rodaParameters.getForcedPreventiveActions();
        assertThat(actions).hasSize(2);
        SwitchAction action1 = (SwitchAction) actions.getFirst();
        SwitchAction action2 = (SwitchAction) actions.getLast();

        assertThat(action1.getId()).isEqualTo("Open Switch1");
        assertThat(action1.getSwitchId()).isEqualTo("Switch1");
        assertThat(action1.isOpen()).isTrue();

        assertThat(action2.getId()).isEqualTo("Close Switch2");
        assertThat(action2.getSwitchId()).isEqualTo("Switch2");
        assertThat(action2.isOpen()).isFalse();
    }

    @Test
    void testReadCracGenerationParameters(@TempDir Path tempDir) throws Exception {
        String jsonString = """
                {
                   "affected_cnec_min_active_power_diff" : 1.0,
                   "affected_cnec_min_loading_diff" : 0.1,
                   "pst_actions_active" : true,
                   "redispatch_actions" : [],
                   "pst_tap_change_max_up" : 8,
                   "pst_tap_change_max_down" : 6,
                   "redispatch_actions_active" : true,
                   "redispatch_generator_required_max_p" : 17.5,
                   "redispatch_activation_cost" : 100.0,
                   "redispatch_cost_up" : 1.0,
                   "redispatch_cost_down" : 1.0,
                   "range_actions_countries" : ["DE", "IT"],
                   "limit_multiplier_preventive" : 1.0,
                   "limit_multiplier_outage" : 1.0,
                   "limit_multiplier_curative" : 1.0
                 }
                """;

        Path jsonFile = Files.writeString(tempDir.resolve("crac_params.json"), jsonString);

        CracGenerationParameters cracParams = InputFilesReader.readCracGenerationParameters(jsonFile.toString());

        assertThat(cracParams.getAffectedCnecMinActivePowerDiff()).isEqualTo(1);
        assertThat(cracParams.getAffectedCnecMinLoadingDiff()).isEqualTo(0.1);

        assertThat(cracParams.isPstActionsActive()).isTrue();
        assertThat(cracParams.getPstTapChangeMaxUp()).isEqualTo(8);
        assertThat(cracParams.getPstTapChangeMaxDown()).isEqualTo(6);

        assertThat(cracParams.isRedispatchActionsActive()).isTrue();
        assertThat(cracParams.getRedispatchActions()).isEmpty();
        assertThat(cracParams.getRedispatchGeneratorRequiredMaxP()).isEqualTo(17.5);
        assertThat(cracParams.getRedispatchActivationCost()).isEqualTo(100);
        assertThat(cracParams.getRedispatchCostUp()).isEqualTo(1);
        assertThat(cracParams.getRedispatchCostDown()).isEqualTo(1);

        assertThat(cracParams.getLimitMultiplierPreventive()).isEqualTo(1);
        assertThat(cracParams.getLimitMultiplierOutage()).isEqualTo(1);
        assertThat(cracParams.getLimitMultiplierCurative()).isEqualTo(1);
        assertThat(cracParams.getRangeActionsCountries()).containsExactlyInAnyOrder(Country.DE, Country.IT);
    }

    @Test
    void testReadRedispatchActions(@TempDir Path tempDir) throws Exception {
        String jsonString = """
                {
                   "redispatch_actions" : [
                        {"id" : "action_1",
                        "generator_distribution_keys" : {"gen1" : 1},
                         "activation_cost" : 100,
                         "variation_cost_up" : 1.2,
                         "variation_cost_down" : 1.5,
                         "active_power_max" : 200,
                         "active_power_min" : 100}
                        ]
                }
                """;

        Path jsonFile = Files.writeString(tempDir.resolve("crac_params.json"), jsonString);

        CracGenerationParameters cracParams = InputFilesReader.readCracGenerationParameters(jsonFile.toString());
        List<RedispatchAction> redispatchActions = cracParams.getRedispatchActions();

        RedispatchAction redispatchAction = redispatchActions.getFirst();

        assertThat(redispatchAction.getGeneratorDistributionKeys()).containsExactlyEntriesOf(Map.of("gen1", 1.));
        assertThat(redispatchAction.getActivationCost()).isEqualTo(100);
        assertThat(redispatchAction.getVariationCostUp()).isEqualTo(1.2);
        assertThat(redispatchAction.getVariationCostDown()).isEqualTo(1.5);
        assertThat(redispatchAction.getActivePowerMax()).isEqualTo(200);
        assertThat(redispatchAction.getActivePowerMin()).isEqualTo(100);
    }

    @Test
    void testReadN1definition() {
        String n1DefinitionPath = InputsReaderTest.class.getResource("/redispatch/2nodes/n1_definition_2nodes.json").getPath();

        ToOpN1Definition n1Definition = InputFilesReader.readN1Definition(n1DefinitionPath);

        assertThat(n1Definition.getMonitoredElements()).hasSize(2);
        ToOpGridElement ge1 = n1Definition.getMonitoredElements().getFirst();
        ToOpGridElement ge2 = n1Definition.getMonitoredElements().getLast();

        assertThat(ge1.getId()).isEqualTo("FRANCE_BELGIUM_1");
        assertThat(ge1.getName()).isEqualTo("France-Belgium interconnection n°1");
        assertThat(ge1.getType()).isEqualTo("LINE");
        assertThat(ge1.getKind()).isEqualTo("branch");

        assertThat(ge2.getId()).isEqualTo("FRANCE_BELGIUM_2");
        assertThat(ge2.getName()).isEqualTo("France-Belgium interconnection n°2");
        assertThat(ge2.getType()).isEqualTo("LINE");
        assertThat(ge2.getKind()).isEqualTo("branch");

        assertThat(n1Definition.getContingencies()).hasSize(3);
        ToOpContingency baseCase = n1Definition.getContingencies().getFirst();
        ToOpContingency co1 = n1Definition.getContingencies().get(1);
        ToOpContingency co2 = n1Definition.getContingencies().getLast();

        assertThat(baseCase.getId()).isEqualTo("BASECASE");
        assertThat(baseCase.getName()).isEmpty();
        assertThat(baseCase.getElements()).isEmpty();

        assertThat(co1.getId()).isEqualTo("CO_FRANCE_BELGIUM_1");
        assertThat(co1.getName()).isEqualTo("CO France-Belgium interconnection n°1");
        assertThat(co1.getElements()).hasSize(1);
        ToOpGridElement co1Ge1 = co1.getElements().getFirst();
        TestAssertUtils.assertGridElementsIdentical(co1Ge1, ge1);

        assertThat(co2.getId()).isEqualTo("CO_FRANCE_BELGIUM_2");
        assertThat(co2.getName()).isEqualTo("CO France-Belgium interconnection n°2");
        ToOpGridElement co2Ge1 = co2.getElements().getFirst();
        TestAssertUtils.assertGridElementsIdentical(co2Ge1, ge2);
    }

    @Test
    void testReadLfResultsParquet() {
        String branchResultsPath = InputsReaderTest.class.getResource("/branch_results.parquet").getPath();
        ToOpLfResult lfResult = InputFilesReader.readLfResult(branchResultsPath);

        assertThat(lfResult.getResults()).hasSize(6);
        assertCnecResult(lfResult.getResults().get(0), "BASECASE", "L1", 2, Double.NaN, -9.68);
        assertCnecResult(lfResult.getResults().get(1), "BASECASE", "L2", 1, 0.282, 9.68);
        assertCnecResult(lfResult.getResults().get(2), "BASECASE", "L2", 2, Double.NaN, -9.68);
        assertCnecResult(lfResult.getResults().get(3), "BASECASE", "L1", 1, 0.282, 9.68);
        assertCnecResult(lfResult.getResults().get(4), "L1", "L2", 1, 0.4596, 15.793);
        assertCnecResult(lfResult.getResults().get(5), "L1", "L2", 2, Double.NaN, -15.793);
    }

    @Test
    void testReadLfResultsJson() {
        String branchResultsPath = InputsReaderTest.class.getResource("/redispatch/2nodes/branch_results.json").getPath();
        ToOpLfResult lfResult = InputFilesReader.readLfResult(branchResultsPath);

        assertThat(lfResult.getResults()).hasSize(4);
        assertCnecResult(lfResult.getResults().get(0), "CO_FRANCE_BELGIUM_1", "FRANCE_BELGIUM_2", 1, 1.6, -800);
        assertCnecResult(lfResult.getResults().get(1), "CO_FRANCE_BELGIUM_2", "FRANCE_BELGIUM_1", 1, 1.6, -800);
        assertCnecResult(lfResult.getResults().get(2), "BASECASE", "FRANCE_BELGIUM_2", 1, 1.07, -533.33);
        assertCnecResult(lfResult.getResults().get(3), "BASECASE", "FRANCE_BELGIUM_1", 1, 0.53, -266.67);
    }

    @Test
    void testReadLfResultsJsonPathNotFound() {
        String branchResultsPath = "/home/non_existing.json";
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> InputFilesReader.readLfResult(branchResultsPath))
                .withMessage("java.nio.file.NoSuchFileException: /home/non_existing.json");
    }

    @Test
    void testReadLfResultsParquetPathNotFound() {
        String branchResultsPath = "/home/non_existing.parquet";
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> InputFilesReader.readLfResult(branchResultsPath))
                .withMessage("java.io.FileNotFoundException: File file:/home/non_existing.parquet does not exist");
    }

    void assertCnecResult(ToOpCnecResult res, String contingency, String element, int side, double loading, double p) {
        assertThat(res.contingency()).isEqualTo(contingency);
        assertThat(res.element()).isEqualTo(element);
        assertThat(res.side()).isEqualTo(side);
        assertThat(res.loading()).isCloseTo(loading, Offset.offset(1e-3));
        assertThat(res.p()).isCloseTo(p, Offset.offset(1e-3));
    }
}
