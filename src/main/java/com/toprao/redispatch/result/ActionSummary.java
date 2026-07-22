package com.toprao.redispatch.result;

import java.time.OffsetDateTime;

public record ActionSummary(String name,
                            String stateId,
                            OffsetDateTime timestamp,
                            ActionType type,
                            double value,
                            double cost) {
}
