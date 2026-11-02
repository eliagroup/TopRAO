package com.toprao.redispatch;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.roda.parameters.RodaParameters;
import com.toprao.crac.CracCreator;
import com.toprao.crac.CracGenerationParameters;
import com.toprao.crac.TopRaoCracCreatorBuilder;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import lombok.NonNull;

import java.util.List;

public class RedispatchComputationBuilder {
    Network network;
    ToOpN1Definition n1Definition;
    ToOpLfResult lfResult = null;
    RodaParameters forcedActions = null;
    RaoParameters raoParameters = null;
    CracGenerationParameters cracGenerationParameters = null;

    public RedispatchComputationBuilder(@NonNull Network network, @NonNull ToOpN1Definition n1Definition) {
        this.network = network;
        this.n1Definition = n1Definition;
    }

    public RedispatchComputationBuilder setLfResult(ToOpLfResult lfResult) {
        this.lfResult = lfResult;
        return this;
    }

    public RedispatchComputationBuilder setForcedActions(RodaParameters forcedActions) {
        this.forcedActions = forcedActions;
        return this;
    }

    public RedispatchComputationBuilder setRaoParameters(RaoParameters raoParameters) {
        this.raoParameters = raoParameters;
        return this;
    }

    public RedispatchComputationBuilder setCracGenerationParameters(CracGenerationParameters cracGenerationParameters) {
        this.cracGenerationParameters = cracGenerationParameters;
        return this;
    }

    public RedispatchComputation build() {
        if (forcedActions == null) {
            forcedActions = new RodaParameters(List.of());
        }

        if (raoParameters == null) {
            raoParameters = RaoParametersFactory.loadDefault();
        }

        CracCreator cracCreator = new TopRaoCracCreatorBuilder(n1Definition)
                .setLfResult(lfResult)
                .setCracGenerationParameters(cracGenerationParameters)
                .setForcedActions(forcedActions)
                .build();

        return new RedispatchComputation(network, forcedActions, raoParameters, cracCreator);
    }
}
