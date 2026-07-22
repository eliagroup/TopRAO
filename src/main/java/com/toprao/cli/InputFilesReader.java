/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.roda.parameters.RodaParameters;
import com.toprao.crac.CracGenerationParameters;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpLfResultParquetReader;
import com.toprao.toop.data.ToOpN1Definition;
import lombok.NonNull;
import org.apache.commons.compress.utils.FileNameUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public final class InputFilesReader {

    public static Network readNetwork(@NonNull String networkPath) {
        Path path = Path.of(networkPath);
        return Network.read(path);
    }

    public static ToOpN1Definition readN1Definition(@NonNull String n1DefinitionPath) {
        Path path = Path.of(n1DefinitionPath);
        return ToOpN1Definition.read(path);
    }

    public static ToOpLfResult readLfResult(@NonNull String lfResultPath) {
        Path path = Path.of(lfResultPath);
        try {
            return readLfResult(path);
        } catch (IOException e) {
            // TODO handle IO exception inside the class
            throw new RuntimeException("IO Exception : " + e.getMessage());
        }
    }

    private static ToOpLfResult readLfResult(Path path) throws IOException {
        String extension = FileNameUtils.getExtension(path.getFileName());
        if (extension.equals("json")) {
            return ToOpLfResult.readJson(path);
        }
        return ToOpLfResultParquetReader.read(path);
    }

    public static RodaParameters readTopologicalAction(String topologicalActions) {
        if (topologicalActions == null) {
            return new RodaParameters(List.of());
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(topologicalActions);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }

        ObjectNode actionsNode = (ObjectNode) rootNode.get("forced-actions").get("preventive-actions-list");
        JsonNode version = actionsNode.get("version");
        actionsNode.set("version", new TextNode(version.asText()));

        String actionsListString;
        try {
            actionsListString = mapper.writeValueAsString(actionsNode);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }

        String raoParamsWithActions = String.format("""
                {
                  "version" : "3.4",
                  "extensions" : {
                      "roda-parameters": {
                        "forced-preventive-actions-list": %s
                      }
                  }
                }
                """, actionsListString);

        InputStream stream = new ByteArrayInputStream(raoParamsWithActions.getBytes(StandardCharsets.UTF_8));

        // Temporary parameters, just to use the reader
        RaoParameters raoParameters = JsonRaoParameters.read(stream, ReportNode.NO_OP);
        RodaParameters rodaParameters = raoParameters.getExtension(RodaParameters.class);
        // Remove the extension to be able to add it as an extension to the RaoParameters used in the computation
        raoParameters.removeExtension(RodaParameters.class);
        return rodaParameters;
    }

    public static RaoParameters readRaoParameters(@NonNull String raoParametersPath) {
        Path path = Path.of(raoParametersPath);
        return JsonRaoParameters.read(path, ReportNode.NO_OP);
    }

    public static CracGenerationParameters readCracGenerationParameters(@NonNull String cracGenerationParametersPath) {
        Path path = Path.of(cracGenerationParametersPath);
        return CracGenerationParameters.read(path);
    }

    private InputFilesReader() {
    }

}
