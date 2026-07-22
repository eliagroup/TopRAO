package com.toprao.crac;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;

public interface CracCreationSpecifier {
    CracCreationParameters getCracCreationParameters(Network network);

    void postprocessCrac(Crac crac);
}
