/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.toop.data;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ToOpLfResult {

    @Getter
    public static class CnecKey {

        private final String element;
        private final String contingency;

        public CnecKey(String element, String contingency) {
            this.element = element;
            this.contingency = contingency;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof CnecKey key) {
                return getElement().equals(key.getElement()) && getContingency().equals(key.getContingency());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getElement(), getContingency());
        }
    }

    @Getter
    private final List<ToOpCnecResult> results;

    private final Map<CnecKey, ToOpCnecResult> index = new HashMap<>();

    private final Map<String, List<ToOpCnecResult>> indexByContingencyId = new HashMap<>();

    private void indexResults() {
        results.forEach(r -> {
            CnecKey cnecKey = new CnecKey(r.element(), r.contingency());
            if (index.containsKey(cnecKey)) {
                ToOpCnecResult alreadyPresent = index.get(cnecKey);
                if (alreadyPresent.loading() < r.loading()) {
                    index.put(cnecKey, r);
                }
            } else {
                index.put(cnecKey, r);
            }
        });
        index.values().forEach(res -> indexByContingencyId.computeIfAbsent(res.contingency(), k -> new ArrayList<>()).add(res));
    }

    public ToOpLfResult(List<ToOpCnecResult> results) {
        this.results = List.copyOf(results);
        indexResults();
    }

    public ToOpCnecResult getLfResult(String elementId, String contingencyId) {
        return index.getOrDefault(new CnecKey(elementId, contingencyId), null);
    }

    public List<ToOpCnecResult> getLfResultsForContingency(String contingencyId) {
        return indexByContingencyId.getOrDefault(contingencyId, null);
    }

}
