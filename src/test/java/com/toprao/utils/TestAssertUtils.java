/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.utils;

import com.toprao.toop.data.ToOpGridElement;

import static org.assertj.core.api.Assertions.assertThat;

public final class TestAssertUtils {

    public static void assertGridElementsIdentical(ToOpGridElement ge1, ToOpGridElement ge2) {
        assertThat(ge1.getId()).isEqualTo(ge2.getId());
        assertThat(ge1.getName()).isEqualTo(ge2.getName());
        assertThat(ge1.getType()).isEqualTo(ge2.getType());
        assertThat(ge1.getKind()).isEqualTo(ge2.getKind());
    }

    private TestAssertUtils() {
    }
}
