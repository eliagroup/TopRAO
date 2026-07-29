/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.cli;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.roda.parameters.RodaParameters;
import com.toprao.JsonUtils;
import com.toprao.crac.CracGenerationParameters;
import com.toprao.redispatch.RaoParametersFactory;
import com.toprao.redispatch.RedispatchComputation;
import com.toprao.redispatch.result.RaoSummary;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.help.HelpFormatter;

import java.nio.file.Path;

@Slf4j
public final class Main {

    private static final String RAO_SUMMARY_FILE = "rao_summary.json";

    private Main() { }

    public static void main(String[] args) {
        try {
            runRedispatch(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    static void runRedispatch(String[] args) {
        RedispatchCliOptions opts = CommandLineHelper.parseCommandLineOptions(args, HelpFormatter.builder().get());
        runRedispatch(opts);
    }

    static void runRedispatch(RedispatchCliOptions opts) {
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

        Path resultsPath = Path.of(opts.outputPath());
        if (resultsPath.toFile().mkdirs()) {
            log.debug("Created results directory {}", resultsPath);
        }

        RaoSummary raoSummary;
        if (opts.lfResultsFilePath().isPresent()) {
            ToOpLfResult lfResult = InputFilesReader.readLfResult(opts.lfResultsFilePath().get());
            lfResult.validateCnecResults(n1Definition);
            raoSummary = RedispatchComputation.compute(network, n1Definition, lfResult, forcedActions, raoParameters, cracGenerationParameters);
        } else {
            raoSummary = RedispatchComputation.compute(network, n1Definition, forcedActions, raoParameters, cracGenerationParameters);
        }
        JsonUtils.write(resultsPath.resolve(RAO_SUMMARY_FILE), raoSummary);
    }

}
