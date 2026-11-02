/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.cli;

import lombok.Getter;
import lombok.NonNull;

@Getter
public final class CracGenerationCliOptions extends AbstractTopRaoCliOptions {

    CracGenerationCliOptions(@NonNull String networkFilePath,
                             @NonNull String n1DefFilePath,
                             @NonNull String outputPath,
                             String lfResultsFilePath,
                             String topologicalAction,
                             String cracGenerationParametersPath) {
        super(networkFilePath, n1DefFilePath, outputPath, lfResultsFilePath, topologicalAction, cracGenerationParametersPath);
    }

}
