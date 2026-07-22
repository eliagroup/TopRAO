package com.toprao.toop.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ToOpCnecResult(@JsonProperty("element") String element,
                             @JsonProperty("contingency") String contingency,
                             @JsonProperty("side") int side,
                             @JsonProperty("loading") double loading,
                             @JsonProperty("p") double p) {
}
