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
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.LineContingency;
import com.powsybl.contingency.TieLineContingency;
import com.powsybl.contingency.TwoWindingsTransformerContingency;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.security.monitor.StateMonitor;
import com.toprao.toop.data.ToOpGridElement;
import com.toprao.toop.data.ToOpN1Definition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class N1DefSaInputsConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(N1DefSaInputsConverter.class);

    private final Network network;

    public record SaInputs(List<Contingency> contingencies, List<StateMonitor> stateMonitors) {
        public SaInputs {
            contingencies = List.copyOf(contingencies);
            stateMonitors = List.copyOf(stateMonitors);
        }
    }

    N1DefSaInputsConverter(Network network) {
        this.network = network;
    }

    public SaInputs convert(ToOpN1Definition n1Definition) {
        return new SaInputs(convertContingencies(n1Definition), convertMonitoredElements(n1Definition));
    }

    private List<Contingency> convertContingencies(ToOpN1Definition n1Definition) {
        return n1Definition.getContingencies().stream()
                .map(c -> {
                    List<ContingencyElement> elements = c.getElements().stream()
                            .map(e -> convertElement(e))
                            .filter(Objects::nonNull)
                            .toList();
                    return new Contingency(c.getId(), c.getName(), elements);
                })
                .toList();
    }

    private List<StateMonitor> convertMonitoredElements(ToOpN1Definition n1Definition) {
        Set<String> branchIds = n1Definition.getMonitoredElements().stream()
                .map(e -> e.getId())
                .filter(id -> network.getBranch(id) != null)
                .collect(Collectors.toSet());
        return List.of(new StateMonitor(ContingencyContext.all(), branchIds, Set.of(), Set.of()));
    }

    private ContingencyElement convertElement(ToOpGridElement toOpElement) {
        Identifiable<?> element = network.getBranch(toOpElement.getId());
        if (element != null) {
            if (element instanceof Line line) {
                return new LineContingency(line.getId());
            } else if (element instanceof TieLine tieLine) {
                return new TieLineContingency(tieLine.getId());
            } else if (element instanceof TwoWindingsTransformer t2wt) {
                return new TwoWindingsTransformerContingency(t2wt.getId());
            }
            LOGGER.warn("Unsupported contingency for type : {}", element.getType());
        } else {
            LOGGER.warn("Element {} not found", toOpElement.getId());
        }

        return null;
    }
}
