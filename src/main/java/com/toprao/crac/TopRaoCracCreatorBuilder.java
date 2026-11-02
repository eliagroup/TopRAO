package com.toprao.crac;

import com.powsybl.openrao.roda.parameters.RodaParameters;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;

import java.util.List;

public class TopRaoCracCreatorBuilder {
    ToOpN1Definition n1Definition;
    ToOpLfResult lfResult = null;
    RodaParameters forcedActions = null;
    CracGenerationParameters cracGenerationParameters = null;

    public TopRaoCracCreatorBuilder(ToOpN1Definition n1Definition) {
        this.n1Definition = n1Definition;
    }

    public TopRaoCracCreatorBuilder setLfResult(ToOpLfResult lfResult) {
        this.lfResult = lfResult;
        return this;
    }

    public TopRaoCracCreatorBuilder setForcedActions(RodaParameters forcedActions) {
        this.forcedActions = forcedActions;
        return this;
    }

    public TopRaoCracCreatorBuilder setCracGenerationParameters(CracGenerationParameters cracGenerationParameters) {
        this.cracGenerationParameters = cracGenerationParameters;
        return this;
    }

    public TopRaoCracCreator build() {
        if (forcedActions == null) {
            forcedActions = new RodaParameters(List.of());
        }

        if (cracGenerationParameters == null) {
            cracGenerationParameters = new CracGenerationParameters();
        }

        return new TopRaoCracCreator(cracGenerationParameters, n1Definition, lfResult, forcedActions);
    }
}
