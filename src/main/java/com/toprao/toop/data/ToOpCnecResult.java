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

public record ToOpCnecResult(@JsonProperty("element") String element,
                             @JsonProperty("contingency") String contingency,
                             @JsonProperty("side") int side,
                             @JsonProperty("loading") double loading,
                             @JsonProperty("p") double p) {
}
