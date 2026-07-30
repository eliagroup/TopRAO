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
public class ToOpN1Definition {

    private final List<ToOpGridElement> monitoredElements;
    private final List<ToOpContingency> contingencies;
    private final String idType;

    @JsonCreator
    public ToOpN1Definition(@JsonProperty("monitored_elements") List<ToOpGridElement> monitoredElements,
                            @JsonProperty("contingencies") List<ToOpContingency> contingencies,
                            @JsonProperty("id_type") String idType) {
        this.monitoredElements = List.copyOf(monitoredElements);
        this.contingencies = List.copyOf(contingencies);
        this.idType = idType;
    }

    public ToOpContingency getContingency(String contingencyId) {
        // TODO add index by id
        return contingencies.stream().filter(c -> c.getId().equals(contingencyId)).findFirst().orElse(null);
    }

}
