package com.toprao.cli;

import java.util.Optional;

public record RedispatchCliOptions(String networkFilePath,
                                   String n1DefFilePath,
                                   Optional<String> lfResultsFilePath,
                                   Optional<String> topologicalAction,
                                   Optional<String> raoParametersFilePath,
                                   Optional<String> cracGenerationParametersPath,
                                   String outputPath) {

}
