/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.cli;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractTopRaoCliOptions {

    @Getter @NonNull
    protected String networkFilePath;
    @Getter @NonNull
    protected String n1DefFilePath;
    @Getter @NonNull
    protected String outputPath;

    protected String lfResultsFilePath;
    protected String topologicalAction;
    protected String cracGenerationParametersPath;

    public Optional<String> getLfResultsFilePath() {
        return Optional.ofNullable(lfResultsFilePath);
    }

    public Optional<String> getTopologicalAction() {
        return Optional.ofNullable(topologicalAction);
    }

    public Optional<String> getCracGenerationParametersPath() {
        return Optional.ofNullable(cracGenerationParametersPath);
    }

}
