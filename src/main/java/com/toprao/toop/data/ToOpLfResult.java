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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
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

    private final Map<CnecKey, ToOpCnecResult> index = new HashMap<>();

    private final Map<String, List<ToOpCnecResult>> indexByContingencyId = new HashMap<>();
    private final Map<String, List<ToOpCnecResult>> indexByElementId = new HashMap<>();

    private void indexResults(List<ToOpCnecResult> results) {
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
        index.values().forEach(
                res -> indexByContingencyId.computeIfAbsent(res.contingency(), k -> new ArrayList<>()).add(res));
        index.values().forEach(
                res -> indexByElementId.computeIfAbsent(res.element(), k -> new ArrayList<>()).add(res));
    }

    private void checkAndFixCnecResults() {
        List<ToOpCnecResult> results = getResults();
        Set<ToOpCnecResult> fixedResults = new HashSet<>();
        for (ToOpCnecResult result : results) {
            double loading = result.loading();
            double p = result.p();

            if (Double.isNaN(loading) && Double.isNaN(p)) {
                throw new IllegalStateException("LF result for element %s at contingency %s has null P and loading"
                        .formatted(result.element(), result.contingency()));
            } else if (Double.isNaN(loading)) {
                ToOpCnecResult completeRes = getCompleteRes(result);
                loading = p * completeRes.loading() / completeRes.p();
            } else if (Double.isNaN(p)) {
                ToOpCnecResult completeRes = getCompleteRes(result);
                p = loading * completeRes.p() / completeRes.loading();
            } else {
                continue;
            }
            fixedResults.add(new ToOpCnecResult(result.element(), result.contingency(), result.side(), loading, p));
        }

        fixedResults.forEach(fixedRes -> replaceResult(fixedRes));
        if (fixedResults.size() > 0) {
            log.warn("{} LF results were missing P or loading, they have been recomputed based on results for same elements", fixedResults.size());
        }
    }

    private ToOpCnecResult getCompleteRes(ToOpCnecResult result) {
        // Get another result for the same element, that has P and loading values
        return indexByElementId.get(result.element()).stream()
                .filter(res -> !Double.isNaN(res.loading()) && !Double.isNaN(res.p()))
                .filter(res -> res.p() != 0 && res.loading() != 0)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("Element /s has null loading and p for all cases"
                                .formatted(result.element())));
    }

    private void replaceResult(ToOpCnecResult result) {
        ToOpCnecResult oldResult = getLfResult(result.element(), result.contingency());
        index.put(new CnecKey(result.element(), result.contingency()), result);

        indexByElementId.get(result.element()).remove(oldResult);
        indexByContingencyId.get(result.contingency()).remove(oldResult);

        indexByContingencyId.computeIfAbsent(result.contingency(), k -> new ArrayList<>()).add(result);
        indexByElementId.computeIfAbsent(result.element(), k -> new ArrayList<>()).add(result);
    }

    public ToOpLfResult(List<ToOpCnecResult> results) {
        indexResults(List.copyOf(results));
        checkAndFixCnecResults();
    }

    public ToOpCnecResult getLfResult(String elementId, String contingencyId) {
        return index.getOrDefault(new CnecKey(elementId, contingencyId), null);
    }

    public List<ToOpCnecResult> getLfResultsForContingency(String contingencyId) {
        return indexByContingencyId.getOrDefault(contingencyId, null);
    }

    public List<ToOpCnecResult> getResults() {
        return index.values().stream().toList();
    }
}
