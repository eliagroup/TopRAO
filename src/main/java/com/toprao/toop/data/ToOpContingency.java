package com.toprao.toop.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class ToOpContingency {
    @JsonProperty("elements")
    private List<ToOpGridElement> elements;
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;

    public ToOpContingency() {
        // used for deserialization
    }

    public ToOpContingency(String id, String name, List<ToOpGridElement> gridElements) {
        this.id = id;
        this.name = name;
        this.elements = gridElements;
    }

}
