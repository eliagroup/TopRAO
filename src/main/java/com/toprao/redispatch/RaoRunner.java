/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.redispatch;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.io.network.NetworkCracCreator;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRao;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.toprao.crac.CracCreationSpecifier;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@NoArgsConstructor
@Getter
public class RaoRunner {

    public static final String TIME_COUPLED_RAO = "TimeCoupledRao";

    private Crac crac = null;

    private TimeCoupledRaoInput timeRaoInput = null;

    public TimeCoupledRaoResult run(Network network, CracCreationSpecifier cracCreationSpecifier, RaoParameters parameters) {
        log.info("Preprocessing network");
        preprocessNetwork(network);
        log.info("Generating Crac");
        crac = generateCrac(cracCreationSpecifier, network);
        cracCreationSpecifier.postprocessCrac(crac);
        log.info("Crac generated");
        log.info("Nb of CNECs : {}", crac.getCnecs().size());
        log.info("Nb of preventive CNECs: {}", crac.getCnecs().stream().filter(c -> c.getState().isPreventive()).count());
        log.info("Nb of curative CNECs : {}", crac.getCnecs().stream().filter(c -> !c.getState().isPreventive()).count());
        log.info("Nb of range actions : {}", crac.getRangeActions().size());

        log.info("Starting RAO computation");
        RaoInput raoInput = RaoInput.build(network, crac).build();
        return run(raoInput, parameters);
    }

    private TimeCoupledRaoResult run(RaoInput raoInput, RaoParameters parameters) {
        OffsetDateTime dateTime = raoInput.getCrac().getTimestamp().orElseThrow();
        Map<OffsetDateTime, RaoInput> timedInputMap = Map.of(dateTime, raoInput);
        timeRaoInput = new TimeCoupledRaoInput(new TemporalDataImpl<>(timedInputMap), new TimeCoupledConstraints());
        return TimeCoupledRao.find(TIME_COUPLED_RAO, ReportNode.NO_OP).run(timeRaoInput, parameters);
    }

    public static Crac generateCrac(CracCreationSpecifier cracCreationSpecifier, Network network) {
        CracCreationContext ccc = NetworkCracCreator.createCrac(network, cracCreationSpecifier.getCracCreationParameters(network));
        ccc.getCreationReport().printCreationReport();
        return ccc.getCrac();
    }

    private static void preprocessNetwork(Network network) {
        for (HvdcLine hvdcLine : network.getHvdcLines()) {
            if (hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class) != null && hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled()) {
                log.info(hvdcLine.getId() + " : deactivating AC emulation");
                hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class).setEnabled(false);
            }
        }
    }
}
