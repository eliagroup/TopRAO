/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.toop.data;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class ToOpLfResult {

    public record CnecKey(String element, String contingency, int side) { }

    private final Map<CnecKey, ToOpCnecResult> index = new HashMap<>();
    private final Map<String, List<ToOpCnecResult>> indexByContingencyId = new HashMap<>();
    private final Map<String, List<ToOpCnecResult>> indexByElementId = new HashMap<>();

    private void indexResults(List<ToOpCnecResult> results) {
        results.forEach(r -> {
            CnecKey cnecKey = new CnecKey(r.element(), r.contingency(), r.side());
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

    public void validateCnecResults(ToOpN1Definition n1Definition) {
        filterCnecsNotInN1Def(n1Definition);
        fixNanValues();
    }

    private void filterCnecsNotInN1Def(ToOpN1Definition n1Definition) {
        List<String> contingencyIds = n1Definition.getContingencies().stream().map(c -> c.getId()).toList();
        List<String> elementIds = n1Definition.getMonitoredElements().stream().map(e -> e.getId()).toList();

        getResults().stream()
                .filter(res -> !contingencyIds.contains(res.contingency()) || !elementIds.contains(res.element()))
                .forEach(res -> removeResult(res));
    }

    private void fixNanValues() {
        Set<ToOpCnecResult> fixedResults = new HashSet<>();
        for (ToOpCnecResult result : getResults()) {
            double loading = result.loading();
            double p = result.p();

            if (!Double.isNaN(loading) && !Double.isNaN(p)) {
                continue;
            }

            if (Double.isNaN(loading) && Double.isNaN(p)) {
                log.error("LF result for element {} at contingency {} has null P and loading. Will be ignored.",
                        result.element(), result.contingency());
                removeResult(result);
            } else {
                Optional<ToOpCnecResult> completeRes = getCompleteResFromOtherCase(result);

                if (completeRes.isEmpty()) {
                    log.error("LF result for element {} at contingency {} has null P or loading, " +
                                    "and cannot be fixed using another result. Will be ignored.",
                            result.element(), result.contingency());
                    removeResult(result);
                    continue;
                }

                if (Double.isNaN(loading)) {
                    loading = p * completeRes.get().loading() / completeRes.get().p(); // !=0 checked before
                } else if (Double.isNaN(p)) {
                    p = loading * completeRes.get().p() / completeRes.get().loading(); // !=0 checked before
                }
            }
            fixedResults.add(new ToOpCnecResult(result.element(), result.contingency(), result.side(), loading, p));
        }

        fixedResults.forEach(fixedRes -> replaceResult(fixedRes));
        if (fixedResults.size() > 0) {
            log.warn("{} LF results were missing P or loading, they have been recomputed based on results for same elements", fixedResults.size());
        }
    }

    private Optional<ToOpCnecResult> getCompleteResFromOtherCase(ToOpCnecResult result) {
        // Get another result for the same element, that has P and loading values
        return indexByElementId.get(result.element()).stream()
                .filter(res -> !Double.isNaN(res.loading()) && !Double.isNaN(res.p()))
                .filter(res -> res.p() != 0 && res.loading() != 0)
                .findFirst();
    }

    private void replaceResult(ToOpCnecResult result) {
        ToOpCnecResult oldResult = getLfResult(result.element(), result.contingency(), result.side());
        index.put(new CnecKey(result.element(), result.contingency(), result.side()), result);

        if (indexByElementId.containsKey(result.element())) {
            indexByElementId.get(result.element()).remove(oldResult);
        }
        if (indexByContingencyId.containsKey(result.contingency())) {
            indexByContingencyId.get(result.contingency()).remove(oldResult);
        }

        indexByContingencyId.computeIfAbsent(result.contingency(), k -> new ArrayList<>()).add(result);
        indexByElementId.computeIfAbsent(result.element(), k -> new ArrayList<>()).add(result);
    }

    private void removeResult(ToOpCnecResult result) {
        index.remove(new CnecKey(result.element(), result.contingency(), result.side()));
        indexByElementId.get(result.element()).remove(result);
        indexByContingencyId.get(result.contingency()).remove(result);
    }

    public ToOpLfResult(List<ToOpCnecResult> results) {
        indexResults(List.copyOf(results));
    }

    public ToOpCnecResult getLfResult(String elementId, String contingencyId, int side) {
        return index.getOrDefault(new CnecKey(elementId, contingencyId, side), null);
    }

    public List<ToOpCnecResult> getLfResultsForContingency(String contingencyId) {
        return indexByContingencyId.getOrDefault(contingencyId, null);
    }

    public List<ToOpCnecResult> getResults() {
        return index.values().stream().toList();
    }
}
