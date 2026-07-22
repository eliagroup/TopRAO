package com.toprao.redispatch.result;

import java.time.OffsetDateTime;

public record CnecSummary(String elementName,
                          String contingencyName,
                          double margin,
                          String unit,
                          String stateId,
                          OffsetDateTime timestamp) { }
