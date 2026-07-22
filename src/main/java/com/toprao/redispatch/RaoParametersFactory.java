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
