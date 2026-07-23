/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.cli;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingOptionException;

public class CommandLineParser extends DefaultParser {
    private final String helpOption;

    public CommandLineParser(final String helpOption) {
        this.helpOption = helpOption;
    }

    @Override
    protected void checkRequiredOptions() throws MissingOptionException {
        // throw if there are required options that have not been processed
        // unless help option has been invoked
        if (!expectedOpts.isEmpty() && !cmd.hasOption(this.helpOption)) {
            throw new MissingOptionException(expectedOpts);
        }
    }
}
