package com.toprao.cli;

import com.toprao.JsonUtils;
import com.toprao.redispatch.result.RaoSummary;
import org.apache.commons.cli.help.HelpAppendable;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.cli.help.TextHelpAppendable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class CliTest {

    @Test
    void runRedispatchWithAllDefaults(@TempDir Path tempPath) {
        String gridPath = getClass().getResource("/redispatch/2nodes/TestCase2Nodes.xiidm").getPath();
        String n1DefinitionPath = getClass().getResource("/redispatch/2nodes/n1_definition_2nodes.json").getPath();

        String[] args = {"-g", gridPath, "-n", n1DefinitionPath, "-o", tempPath.toAbsolutePath().toString()};
        Main.runRedispatch(args);

        RaoSummary raoSummary = JsonUtils.read(tempPath.resolve("rao_summary.json"), RaoSummary.class);

        assertThat(raoSummary.isSecure()).isTrue();
        assertThat(raoSummary.getFunctionalCost()).isEqualTo(820);
        assertThat(raoSummary.getActions()).hasSize(2);
        assertThat(raoSummary.getLimitingElements()).hasSize(4);
    }

    @Test
    void runRedispatchWithAllOptionalArgs(@TempDir Path tempPath) {
        String gridPath = getClass().getResource("/redispatch/2nodes/TestCase2Nodes.xiidm").getPath();
        String n1DefinitionPath = getClass().getResource("/redispatch/2nodes/n1_definition_2nodes.json").getPath();
        String lfResulstPath = getClass().getResource("/redispatch/2nodes/branch_results.json").getPath();
        String raoParametersPath = getClass().getResource("/redispatch/2nodes/RaoParameters.json").getPath();
        String cracGenerationParametersPath = getClass().getResource("/redispatch/2nodes/crac_parameters.json").getPath();

        String s = """
                {
                    "forced-actions": {
                        "preventive-actions-list": {
                            "version" : "1.3",
                            "actions" : [ {
                                "type" : "TERMINALS_CONNECTION",
                                "id" : "open_FRANCE_BELGIUM_1",
                                "elementId" : "FRANCE_BELGIUM_1",
                                "open" : true
                            }]
                        }
                    }
                }
                """;

        String[] args = {"-g", gridPath,
            "-n", n1DefinitionPath,
            "-l", lfResulstPath,
            "-t", s,
            "-r", raoParametersPath,
            "-c", cracGenerationParametersPath,
            "-o", tempPath.toAbsolutePath().toString()};
        Main.runRedispatch(args);

        RaoSummary raoSummary = JsonUtils.read(tempPath.resolve("rao_summary.json"), RaoSummary.class);

        assertThat(raoSummary.isSecure()).isTrue();
        assertThat(raoSummary.getFunctionalCost()).isEqualTo(1440); // costs up and down are set to 2
        assertThat(raoSummary.getActions()).hasSize(2);
        assertThat(raoSummary.getLimitingElements()).hasSize(4);
    }

    @Test
    void testHelpMessage() {
        StringBuilder output = new StringBuilder();
        HelpAppendable helpAppendable = new TextHelpAppendable(output);

        HelpFormatter formatter = HelpFormatter.builder()
                .setHelpAppendable(helpAppendable)
                .get();

        String[] args = {"-h"};
        CommandLineHelper.parseCommandLineOptions(args, formatter);
        String helpOutput = output.toString();
        assertThat(helpOutput).isEqualTo("""
              usage:  java -jar TopRao.jar [-c <arg>] -g <arg> [-h] [-l <arg>] -n <arg>
                 -o <arg> [-r <arg>] [-t <arg>]

                             Options                     Since           Description      \s
              -h, --help                                  --       Print this help message\s
              -g, --grid <arg>                            --       Path to the input grid \s
                                                                    IIDM file             \s
              -n, --n1_definition <arg>                   --       Path to the N-1        \s
                                                                    definition JSON file  \s
              -l, --lf_result <arg>                       --       Path to the load flow  \s
                                                                    results parquet file  \s
                                                                    (optional)            \s
              -t, --topological_action <arg>              --       String in JSON format of
                                                                    the forced preventive \s
                                                                    topological actions   \s
                                                                    (optional)            \s
              -r, --rao_parameters <arg>                  --       Path to the RAO        \s
                                                                    parameters JSON file  \s
                                                                    (optional)            \s
              -c, --crac_generation_parameters <arg>      --       Path to the crac       \s
                                                                    generation parameters \s
                                                                    JSON file (optional)  \s
              -o, --output <arg>                          --       Path to the desired    \s
                                                                    output directory      \s

             """);
    }
}
