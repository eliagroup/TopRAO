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
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

public final class RaoParametersFactory {

    private static final String DEFAULT_RAO_PARAMETERS_FILE = "/redispatch/DefaultRaoParameters.json";

    private RaoParametersFactory() { }

    public static RaoParameters loadDefault() {
        return JsonRaoParameters.read(RaoParametersFactory.class.getResourceAsStream(DEFAULT_RAO_PARAMETERS_FILE), ReportNode.NO_OP);
    }

}
