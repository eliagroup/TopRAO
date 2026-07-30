/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.toop.data;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.ParquetReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ToOpLfResultParquetReader {

    private ToOpLfResultParquetReader() { }

    public static ToOpLfResult read(Path path) {
        try {
            Configuration conf = new Configuration();
            org.apache.hadoop.fs.Path hPath = new org.apache.hadoop.fs.Path(path.toUri());

            List<ToOpCnecResult> results = new ArrayList<>();
            try (ParquetReader<ToOpCnecResult> reader = ParquetReader.builder(new CnecResultParquetReadSupport(), hPath)
                    .withConf(conf)
                    .build()) {
                ToOpCnecResult r;
                while ((r = reader.read()) != null) {
                    results.add(r);
                }
            }

            return new ToOpLfResult(results);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }
}
