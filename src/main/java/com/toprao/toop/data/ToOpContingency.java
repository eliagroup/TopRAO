/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.toop.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class ToOpContingency {
    private final String id;
    private final String name;
    private final List<ToOpGridElement> elements;

    @JsonCreator
    public ToOpContingency(@JsonProperty("id") String id,
                           @JsonProperty("name") String name,
                           @JsonProperty("elements") List<ToOpGridElement> elements) {
        this.id = id;
        this.name = name;
        this.elements = List.copyOf(elements);
    }

}
