/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.utils;

import com.powsybl.iidm.network.Network;

public final class NetworkImportsUtil {

    public static Network import2NodesNetwork() {
        return Network.read(
            "TestCase2Nodes_withLF.xiidm",
            NetworkImportsUtil.class.getResourceAsStream(
                "/redispatch/2nodes/TestCase2Nodes.xiidm"
            )
        );
    }

    private NetworkImportsUtil() { }
}
