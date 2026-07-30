/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.redispatch.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class RaoSummary {

    private boolean isSecure;
    private double functionalCost;
    private List<ActionSummary> actions;
    private List<CnecSummary> limitingElements;

    @JsonCreator
    public RaoSummary(@JsonProperty("secure") boolean isSecure,
                      @JsonProperty("functional_cost") double functionalCost,
                      @JsonProperty("actions") List<ActionSummary> actions,
                      @JsonProperty("limiting_elements")List<CnecSummary> limitingElements) {
        this.isSecure = isSecure;
        this.functionalCost = functionalCost;
        this.actions = actions;
        this.limitingElements = limitingElements;
    }

}
