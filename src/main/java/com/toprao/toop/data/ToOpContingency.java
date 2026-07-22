/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

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
