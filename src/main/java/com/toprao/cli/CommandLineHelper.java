package com.toprao.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.Optional;

public final class CommandLineHelper {

    public static final String CMD_LINE_SYNTAX = "java -jar TopRao.jar";
    public static final String HELP_OPT = "help";
    public static final String GRID_OPT = "grid";
    public static final String N1DEF_OPT = "n1_definition";
    public static final String LF_RESULT_OPT = "lf_result";
    public static final String TOPO_ACTION_OPT = "topological_action";

    public static final String RAO_PARAMETERS_OPT = "rao_parameters";

    public static final String CRAC_GENERATION_PARAMETERS_OPT = "crac_generation_parameters";

    public static final String OUTPUT_OPT = "output";

    private CommandLineHelper() { }

    public static RedispatchCliOptions parseCommandLineOptions(String[] args) {
        Options options = buildCommandLineOptions();

        org.apache.commons.cli.CommandLineParser parser = new CommandLineParser(HELP_OPT);
        // TODO replace with org.apache.commons.cli.help.HelpFormatter
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            formatter.printHelp(CMD_LINE_SYNTAX, options);
            System.out.println(e.getMessage());
            System.exit(1);
        }

        if (cmd.hasOption(HELP_OPT)) {
            formatter.printHelp(CMD_LINE_SYNTAX, options);
            System.exit(0);
        }

        String networkFilePath = readRequired(cmd, GRID_OPT);
        String n1DefFilePath = readRequired(cmd, N1DEF_OPT);

        Optional<String> lfResultsFilePath = readOptional(cmd, LF_RESULT_OPT);
        Optional<String> topologicalActionFilePath = readOptional(cmd, TOPO_ACTION_OPT);
        Optional<String> raoParametersFilePath = readOptional(cmd, RAO_PARAMETERS_OPT);
        Optional<String> cracGenerationParametersFilePath = readOptional(cmd, CRAC_GENERATION_PARAMETERS_OPT);

        String outputPath = readRequired(cmd, OUTPUT_OPT);

        return new RedispatchCliOptions(networkFilePath, n1DefFilePath, lfResultsFilePath, topologicalActionFilePath, raoParametersFilePath, cracGenerationParametersFilePath, outputPath);
    }

    private static String readRequired(CommandLine cmd, String optName) {
        String networkFilePath = cmd.getOptionValue(optName);
        checkRequiredOption(networkFilePath, optName);
        return networkFilePath;
    }

    private static void checkRequiredOption(String input, String optName) {
        if (input == null) {
            System.err.printf("You must define --%s. Use --help for extra information.%n", optName);
            System.exit(1);
        }
    }

    private static Optional<String> readOptional(CommandLine cmd, String option) {
        Optional<String> input = Optional.empty();
        if (cmd.hasOption(option)) {
            input = Optional.of(cmd.getOptionValue(option));
        }
        return input;
    }

    private static Options buildCommandLineOptions() {
        Options options = new Options();
        Option helpOption = new Option(HELP_OPT.substring(0, 1), HELP_OPT, false, "Print this help message");
        options.addOption(helpOption);

        Option gridOpt = new Option(GRID_OPT.substring(0, 1), GRID_OPT, true, "Path to the input grid IIDM file");
        gridOpt.setRequired(true);
        options.addOption(gridOpt);

        Option n1DefOpt = new Option(N1DEF_OPT.substring(0, 1), N1DEF_OPT, true, "Path to the N-1 definition JSON file");
        n1DefOpt.setRequired(true);
        options.addOption(n1DefOpt);

        Option lfResultOpt = new Option(LF_RESULT_OPT.substring(0, 1), LF_RESULT_OPT, true, "Path to the load flow results parquet file (optional)");
        lfResultOpt.setRequired(false);
        options.addOption(lfResultOpt);

        Option topologicalActionOpt = new Option(TOPO_ACTION_OPT.substring(0, 1), TOPO_ACTION_OPT, true, "String in JSON format of the forced preventive topological actions (optional)");
        topologicalActionOpt.setRequired(false);
        options.addOption(topologicalActionOpt);

        Option raoParametersOpt = new Option(RAO_PARAMETERS_OPT.substring(0, 1), RAO_PARAMETERS_OPT, true, "Path to the RAO parameters JSON file (optional)");
        raoParametersOpt.setRequired(false);
        options.addOption(raoParametersOpt);

        Option cracGenerationOpt = new Option(CRAC_GENERATION_PARAMETERS_OPT.substring(0, 1), CRAC_GENERATION_PARAMETERS_OPT, true, "Path to the crac generation parameters JSON file (optional)");
        cracGenerationOpt.setRequired(false);
        options.addOption(cracGenerationOpt);

        Option outputOption = new Option(OUTPUT_OPT.substring(0, 1), OUTPUT_OPT, true, "Path to the desired output directory");
        outputOption.setRequired(true);
        options.addOption(outputOption);

        return options;
    }
}
