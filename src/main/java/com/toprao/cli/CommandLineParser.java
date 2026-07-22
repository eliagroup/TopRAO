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
