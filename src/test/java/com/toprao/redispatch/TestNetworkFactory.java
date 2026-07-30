package com.toprao.redispatch;

import com.powsybl.iidm.network.Network;
import com.powsybl.openloadflow.network.FourBusNetworkFactory;
import com.powsybl.openloadflow.network.PhaseControlFactory;

public final class TestNetworkFactory {

    /**
     * <p>4 bus test network:</p>
     *<pre>
     *     g1 -30MW              d2 20MW
     *   1 =======           2 =======
     *      | | |               |   |
     *      | | +---------------+   |
     *      | |                     |
     *      | +-------------------+ |
     *      |                     | |
     *      |   +---------------+ | |
     *      |   |               | | |
     *   4 =======           3 =======
     *    g4 -40MW             d3 50MW
     *</pre>
     *
     */

    static Network createFourBusNetwork() {
        Network network = FourBusNetworkFactory.createBaseNetwork();
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup1().newActivePowerLimits().setPermanentLimit(50).add();
        network.getLine("l14").getOrCreateSelectedOperationalLimitsGroup2().newActivePowerLimits().setPermanentLimit(50).add();
        network.getGenerator("g1").setTargetP(30).setMaxP(50).setMinP(10);
        network.getGenerator("g4").setTargetP(40).setMaxP(60).setMinP(20);

        network.getLoad("d2").setP0(20);
        network.getLoad("d3").setP0(50);

        network.getLine("l34").remove();

        return network;
    }

    /**
     * <pre>
     * A very small network to test a phase shifter on a T2wt.
     *
     *     G1                   LD2
     *     |          L1        |
     *     |  ----------------- |
     *     B1                   B2
     *        --------B3-------
     *           PS1       L2
     * </pre>
     */
    static Network createWithPst() {
        Network network = PhaseControlFactory.createNetworkWithT2wt();
        network.getLine("L1").getOrCreateSelectedOperationalLimitsGroup1().newActivePowerLimits().setPermanentLimit(50).add();
        network.getLine("L1").getOrCreateSelectedOperationalLimitsGroup2().newActivePowerLimits().setPermanentLimit(50).add();
        network.getLine("L2").getOrCreateSelectedOperationalLimitsGroup1().newActivePowerLimits().setPermanentLimit(50).add();
        network.getLine("L2").getOrCreateSelectedOperationalLimitsGroup2().newActivePowerLimits().setPermanentLimit(50).add();
        network.getTwoWindingsTransformer("PS1").getOrCreateSelectedOperationalLimitsGroup1().newActivePowerLimits().setPermanentLimit(50).add();
        network.getTwoWindingsTransformer("PS1").getOrCreateSelectedOperationalLimitsGroup2().newActivePowerLimits().setPermanentLimit(50).add();
        return network;
    }

    private TestNetworkFactory() {
    }

}
