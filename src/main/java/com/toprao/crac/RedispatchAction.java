package com.toprao.crac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor // for deserialization
@Builder
@Setter
@Getter
public class RedispatchAction {
    private String generatorId;
    private double activationCost = Double.NaN;
    private double variationCostUp = Double.NaN;
    private double variationCostDown = Double.NaN;
    private double activePowerMax = Double.NaN;
    private double activePowerMin = Double.NaN;
}
