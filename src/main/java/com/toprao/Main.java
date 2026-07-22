package com.toprao;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.roda.parameters.RodaParameters;
import com.toprao.cli.CommandLineHelper;
import com.toprao.cli.InputFilesReader;
import com.toprao.cli.RedispatchCliOptions;
import com.toprao.crac.CracGenerationParameters;
import com.toprao.redispatch.RaoParametersFactory;
import com.toprao.redispatch.RedispatchComputation;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public final class Main {

    private Main() { }

    public static void main(String[] args) {
        try {
            runRedispatch(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void runRedispatch(String[] args) {
        RedispatchCliOptions opts = CommandLineHelper.parseCommandLineOptions(args);

        Network network = InputFilesReader.readNetwork(opts.networkFilePath());
        ToOpN1Definition n1Definition = InputFilesReader.readN1Definition(opts.n1DefFilePath());

        RodaParameters forcedActions = InputFilesReader.readTopologicalAction(opts.topologicalAction().orElse(null));
        RaoParameters raoParameters;
        if (opts.raoParametersFilePath().isPresent()) {
            raoParameters = InputFilesReader.readRaoParameters(opts.raoParametersFilePath().get());
        } else {
            raoParameters = RaoParametersFactory.loadDefault();
        }

        CracGenerationParameters cracGenerationParameters;
        if (opts.cracGenerationParametersPath().isPresent()) {
            cracGenerationParameters = InputFilesReader.readCracGenerationParameters(opts.cracGenerationParametersPath().get());
        } else {
            cracGenerationParameters = new CracGenerationParameters();
        }

        if (opts.lfResultsFilePath().isPresent()) {
            ToOpLfResult lfResult = InputFilesReader.readLfResult(opts.lfResultsFilePath().get());
            RedispatchComputation.compute(network, n1Definition, lfResult, forcedActions, raoParameters, cracGenerationParameters, Path.of(opts.outputPath()));
        } else {
            RedispatchComputation.compute(network, n1Definition, forcedActions, raoParameters, cracGenerationParameters, Path.of(opts.outputPath()));
        }
    }

}
