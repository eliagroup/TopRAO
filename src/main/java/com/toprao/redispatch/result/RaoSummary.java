package com.toprao.redispatch.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toprao.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

@AllArgsConstructor
@Getter
public class RaoSummary {

    @JsonProperty("secure")
    private boolean isSecure;

    @JsonProperty("functional_cost")
    private double functionalCost;

    @JsonProperty("actions")
    private List<ActionSummary> actions;

    @JsonProperty("limiting_elements")
    private List<CnecSummary> limitingElements;

    public void write(Path p) {
        try {
            JsonUtils.getObjectMapper().writeValue(p.toFile(), this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
