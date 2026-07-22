package com.toprao.toop.data;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.ParquetReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ToOpLfResultParquetReader {

    private ToOpLfResultParquetReader() { }

    public static ToOpLfResult read(Path path) throws IOException {
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
    }
}
