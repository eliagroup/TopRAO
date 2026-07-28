package com.toprao.redispatch;

import com.powsybl.iidm.network.Network;
import com.powsybl.openloadflow.network.FourBusNetworkFactory;

public final class FourBusRaoNetworkFactory {

    static Network createBaseNetwork() {
        Network network = FourBusNetworkFactory.createBaseNetwork();
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup1().newActivePowerLimits().setPermanentLimit(50).add();
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup2().newActivePowerLimits().setPermanentLimit(50).add();
        network.getGenerator("g1").setTargetP(30).setMaxP(50).setMinP(10);
        network.getGenerator("g4").setTargetP(40).setMaxP(60).setMinP(20);

        return network;
    }

    private FourBusRaoNetworkFactory() {
    }

}
