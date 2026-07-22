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
