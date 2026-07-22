package com.toprao.redispatch;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.roda.parameters.RodaParameters;
import com.toprao.crac.CracGenerationParameters;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.nio.file.Path;

@Builder
@Getter
public class RedispatchParameters {
    @NonNull private Network network;
    @NonNull private ToOpN1Definition n1Definition;
    private ToOpLfResult lfResult;
    private RodaParameters forcedActions;
    private RaoParameters raoParameters;
    private CracGenerationParameters cracGenerationParameters;
    private Path resultsPath;
}
