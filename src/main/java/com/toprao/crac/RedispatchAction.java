/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.crac;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class RedispatchAction {
    private final String id;
    Map<String, Double> generatorDistributionKeys;
    private double activationCost;
    private double variationCostUp;
    private double variationCostDown;
    private double activePowerMax;
    private double activePowerMin;

    public RedispatchAction(String id, String generatorId, double activationCost, double variationCostUp,
                            double variationCostDown, double activePowerMax, double activePowerMin) {
        this(id, Map.of(generatorId, 1.), activationCost, variationCostUp, variationCostDown, activePowerMax, activePowerMin);
    }

    @JsonCreator
    public RedispatchAction(
            @JsonProperty(value = "id", required = true) String id,
            @JsonProperty(value = "generator_distribution_keys", required = true) Map<String, Double> generatorDistributionKeys,
            @JsonProperty(value = "activation_cost", required = true) double activationCost,
            @JsonProperty(value = "variation_cost_up", required = true) double variationCostUp,
            @JsonProperty(value = "variation_cost_down", required = true) double variationCostDown,
            @JsonProperty(value = "active_power_max", required = true) double activePowerMax,
            @JsonProperty(value = "active_power_min", required = true) double activePowerMin) {
        this.id = id;
        this.generatorDistributionKeys = new HashMap<>(generatorDistributionKeys);
        this.activationCost = activationCost;
        this.variationCostUp = variationCostUp;
        this.variationCostDown = variationCostDown;
        this.activePowerMax = activePowerMax;
        this.activePowerMin = activePowerMin;
    }

}
