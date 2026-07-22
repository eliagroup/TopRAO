package com.toprao.toop.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ToOpGridElement {

    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("type")
    private String type;
    @JsonProperty("kind")
    private String kind;

    public ToOpGridElement() {
        // used for deserialization
    }

    public ToOpGridElement(String id, String name, String type, String kind) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.kind = kind;
    }

}
