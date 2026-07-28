package com.toprao.cli;

import org.apache.commons.cli.help.HelpAppendable;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.cli.help.TextHelpAppendable;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CliTest {
    @Test
    public void testHelpMessage() throws IOException {
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
