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
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.roda.parameters.RodaParameters;
import com.toprao.JsonUtils;
import com.toprao.crac.CracGenerationParameters;
import com.toprao.crac.TopRaoCracCreator;
import com.toprao.crac.TopRaoCracCreatorBuilder;
import com.toprao.redispatch.RedispatchComputation;
import com.toprao.redispatch.RedispatchComputationBuilder;
import com.toprao.redispatch.result.RaoSummary;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.help.HelpFormatter;

import java.io.FileOutputStream;
import java.nio.file.Path;

@Slf4j
public final class Main {

    private static final String RAO_SUMMARY_FILE = "rao_summary.json";

    private Main() { }

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
//                CommandLineHelper.printHelp();
                return;
            }

            String command = args[0];
            String[] cmdArgs = java.util.Arrays.copyOfRange(args, 1, args.length);

            switch (command) {
                case "crac":
                    generateCrac(cmdArgs);
                    break;
                case "redispatch":
                    runRedispatch(cmdArgs);
                    break;
                default:
                    log.error("Supported commands are \"crac\" or \"redispatch\"");

            }
        } catch (Exception e) {
            log.error(e.getMessage());
            System.exit(1);
        }
    }

    static void runRedispatch(String[] args) {
        RedispatchCliOptions opts = CommandLineHelper.parseRedispatchCommandLineOptions(args, HelpFormatter.builder().get());
        runRedispatch(opts);
    }

    static void runRedispatch(RedispatchCliOptions opts) {
        Network network = InputFilesReader.readNetwork(opts.getNetworkFilePath());
        ToOpN1Definition n1Definition = InputFilesReader.readN1Definition(opts.getN1DefFilePath());

        RedispatchComputationBuilder builder = new RedispatchComputationBuilder(network, n1Definition);

        RodaParameters forcedActions = InputFilesReader.readTopologicalAction(opts.getTopologicalAction()
                .orElse(null));
        builder.setForcedActions(forcedActions);

        if (opts.getRaoParametersFilePath().isPresent()) {
            RaoParameters raoParameters = InputFilesReader.readRaoParameters(opts.getRaoParametersFilePath().get());
            builder.setRaoParameters(raoParameters);
        }

        if (opts.getCracGenerationParametersPath().isPresent()) {
            CracGenerationParameters cracGenerationParameters = InputFilesReader.readCracGenerationParameters(opts.getCracGenerationParametersPath().get());
            builder.setCracGenerationParameters(cracGenerationParameters);
        }

        if (opts.getLfResultsFilePath().isPresent()) {
            ToOpLfResult lfResult = InputFilesReader.readLfResult(opts.lfResultsFilePath);
            builder.setLfResult(lfResult);
        }

        Path resultsPath = Path.of(opts.getOutputPath());
        if (resultsPath.toFile().mkdirs()) {
            log.debug("Created results directory {}", resultsPath);
        }

        RedispatchComputation computation = builder.build();
        RaoSummary raoSummary = computation.compute();

        JsonUtils.write(resultsPath.resolve(RAO_SUMMARY_FILE), raoSummary);
    }

    static void generateCrac(String[] args) {
        CracGenerationCliOptions opts = CommandLineHelper.parseCracGenerationCommandLineOptions(args, HelpFormatter.builder().get());
        generateCrac(opts);
    }

    static void generateCrac(CracGenerationCliOptions opts) {
        // todo move logs elsewhere
        log.info("Generating Crac");
        Network network = InputFilesReader.readNetwork(opts.getNetworkFilePath());
        ToOpN1Definition n1Definition = InputFilesReader.readN1Definition(opts.getN1DefFilePath());

        TopRaoCracCreatorBuilder builder = new TopRaoCracCreatorBuilder(n1Definition);
        if (opts.getCracGenerationParametersPath().isPresent()) {
            CracGenerationParameters cracGenerationParameters = InputFilesReader.readCracGenerationParameters(opts.getCracGenerationParametersPath().get());
            builder.setCracGenerationParameters(cracGenerationParameters);
        }

        if (opts.getLfResultsFilePath().isPresent()) {
            ToOpLfResult lfResult = InputFilesReader.readLfResult(opts.lfResultsFilePath);
            builder.setLfResult(lfResult);
        }

        TopRaoCracCreator cracCreator = builder.build();
        Crac crac = cracCreator.create(network);

        // todo move elsewhere
        log.info("Crac generated");
        log.info("Nb of CNECs : {}", crac.getCnecs().size());
        log.info("Nb of preventive CNECs: {}", crac.getCnecs().stream().filter(c -> c.getState().isPreventive()).count());
        log.info("Nb of curative CNECs : {}", crac.getCnecs().stream().filter(c -> !c.getState().isPreventive()).count());
        log.info("Nb of injection actions : {}", crac.getInjectionRangeActions().size());
        log.info("Nb of pst actions : {}", crac.getPstRangeActions().size());

        // todo write crac
        try (FileOutputStream outputStream = new FileOutputStream(opts.getOutputPath() + "/crac.json")) {
            crac.write("JSON", outputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
