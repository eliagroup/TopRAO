package com.toprao.crac;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;

public interface CracCreator {
    Crac generateCrac(Network network);

}
