/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.redispatch;

import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.VoltageLevel;
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

    static Network createWithPstAndTieLine() {
        Network network = createWithPst();
        VoltageLevel vl1 = network.getVoltageLevel("VL1");
        VoltageLevel vl2 = network.getVoltageLevel("VL2");

        TwoSides boundarySide1 = TwoSides.ONE;
        TwoSides boundarySide2 = TwoSides.TWO;
        BoundaryLine dl1 = vl1.newBoundaryLine()
                .setId(boundarySide1.name())
                .setName(boundarySide1.name())
                .setP0(0.0)
                .setQ0(0.0)
                .setR(1.0)
                .setX(2.0)
                .setBus("B1")
                .setPairingKey("key")
                .add();
        BoundaryLine dl2 = vl2.newBoundaryLine()
                .setId(boundarySide2.name())
                .setName(boundarySide2.name())
                .setP0(0.0)
                .setQ0(0.0)
                .setR(1.0)
                .setX(2.0)
                .setBus("B2")
                .setPairingKey("key")
                .add();

        dl1.getOrCreateSelectedOperationalLimitsGroup().newCurrentLimits().setPermanentLimit(500).add();
        dl2.getOrCreateSelectedOperationalLimitsGroup().newCurrentLimits().setPermanentLimit(500).add();

        network.newTieLine()
                .setId(boundarySide1.name() + " + " + boundarySide2.name())
                .setName(boundarySide1.name() + " + " + boundarySide2.name())
                .setBoundaryLine1(dl1.getId())
                .setBoundaryLine2(dl2.getId())
                .add();

        return network;
    }

    private TestNetworkFactory() {
    }

}
