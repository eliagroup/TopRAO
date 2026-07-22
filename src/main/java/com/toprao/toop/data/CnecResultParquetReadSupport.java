package com.toprao.toop.data;

import org.apache.parquet.hadoop.api.InitContext;
import org.apache.parquet.hadoop.api.ReadSupport;
import org.apache.parquet.io.api.RecordMaterializer;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;

import java.util.Map;

public class CnecResultParquetReadSupport extends ReadSupport<ToOpCnecResult> {
    // requested schema matching the record fields
    private static final MessageType SCHEMA = MessageTypeParser.parseMessageType(
            "message to_op_cnec_result { "
                    + "optional binary element (UTF8); "
                    + "optional binary contingency (UTF8); "
                    + "optional int64 side; "
                    + "optional double loading; "
                    + "optional double p; "
                    + "}"
    );

    @Override
    public ReadContext init(InitContext context) {
        return new ReadContext(SCHEMA);
    }

    @Override
    public RecordMaterializer<ToOpCnecResult> prepareForRead(
            org.apache.hadoop.conf.Configuration conf,
            Map<String, String> keyValueMetaData,
            MessageType fileSchema,
            ReadContext readContext) {

        return new CnecResultParquetMaterializer(readContext.getRequestedSchema(), ToOpCnecResult.class);
    }
}
