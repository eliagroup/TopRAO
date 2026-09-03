/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

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
