/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.sa;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.ac.solver.StateVectorScalingMode;
import com.powsybl.openloadflow.sa.OpenSecurityAnalysisParameters;
import com.powsybl.security.SecurityAnalysis;
import com.powsybl.security.SecurityAnalysisParameters;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.SecurityAnalysisRunParameters;
import com.powsybl.security.monitor.StateMonitor;
import com.toprao.toop.data.ToOpN1Definition;

import java.util.List;

public class SecurityAnalysisRunner {

    public static LoadFlowParameters createLfParameters() {
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        OpenLoadFlowParameters olfParams = OpenLoadFlowParameters.load(loadFlowParameters);
        olfParams.setMaxNewtonRaphsonIterations(60)
                .setStateVectorScalingMode(StateVectorScalingMode.MAX_VOLTAGE_CHANGE)
                .setMaxVoltageChangeStateVectorScalingMaxDv(0.1)
                .setVoltageInitModeOverride(OpenLoadFlowParameters.VoltageInitModeOverride.FULL_VOLTAGE)
                .setVoltageRemoteControlRobustMode(true);
        return loadFlowParameters;
    }

    public SecurityAnalysisResult run(Network network, ToOpN1Definition n1Definition) {
        return run(network, n1Definition, 1);
    }

    public SecurityAnalysisResult run(Network network, ToOpN1Definition n1Definition, int threadCount) {
        LoadFlowParameters loadFlowParameters = createLfParameters();
        return run(network, n1Definition, loadFlowParameters, threadCount);
    }

    public SecurityAnalysisResult run(Network network, ToOpN1Definition n1Definition, LoadFlowParameters loadFlowParameters, int threadCount) {
        N1DefSaInputsConverter.SaInputs saInputs = new N1DefSaInputsConverter(network).convert(n1Definition);
        return run(network, saInputs.contingencies(), saInputs.stateMonitors(), loadFlowParameters, threadCount);
    }

    public SecurityAnalysisResult run(Network network, List<Contingency> contingencies, List<StateMonitor> stateMonitors, LoadFlowParameters loadFlowParameters, int threadCount) {
        SecurityAnalysisParameters securityAnalysisParameters = new SecurityAnalysisParameters()
                .setLoadFlowParameters(loadFlowParameters);
        SecurityAnalysisRunParameters runParameters = new SecurityAnalysisRunParameters().setSecurityAnalysisParameters(securityAnalysisParameters);
        OpenSecurityAnalysisParameters openSecurityAnalysisParameters = OpenSecurityAnalysisParameters.getOrDefault(runParameters.getSecurityAnalysisParameters());
        runParameters.getSecurityAnalysisParameters().addExtension(OpenSecurityAnalysisParameters.class, openSecurityAnalysisParameters);
        openSecurityAnalysisParameters
                .setDcFastMode(true)
                .setContingencyPropagation(false)
                .setThreadCount(threadCount);

        runParameters.setMonitors(stateMonitors);

        return run(network, contingencies, runParameters);
    }

    private SecurityAnalysisResult run(Network network, List<Contingency> contingencies, SecurityAnalysisRunParameters parameters) {
        return SecurityAnalysis.run(network, contingencies, parameters).getResult();
    }

}
