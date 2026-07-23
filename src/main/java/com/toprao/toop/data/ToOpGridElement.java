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

@Getter
public class ToOpGridElement {

    private final String id;
    private final String name;
    private final String type;
    private final String kind;

    @JsonCreator
    public ToOpGridElement(@JsonProperty("id") String id,
                           @JsonProperty("name") String name,
                           @JsonProperty("type") String type,
                           @JsonProperty("kind") String kind) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.kind = kind;
    }

}
