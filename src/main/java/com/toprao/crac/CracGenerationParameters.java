/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.crac;

import com.powsybl.iidm.network.Country;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
public class CracGenerationParameters {
    public static final Set<Country> ALL_COUNTRIES_SET = Set.of(Country.values());

    private static double AFFECTED_CNEC_MIN_ACTIVE_POWER_DIFF_DEFAULT_VALUE = 5;
    private static double AFFECTED_CNEC_MIN_LOADING_DIFF_DEFAULT_VALUE = 0.1;

    private static boolean PST_ACTIONS_DEFAULT_VALUE = true;
    private static int PST_TAP_MAX_CHANGE_UP_DEFAULT_VALUE = 3;
    private static int PST_TAP_MAX_CHANGE_DOWN_DEFAULT_VALUE = 3;

    private static boolean REDISPATCH_ACTIONS_DEFAULT_VALUE = true;
    private static double REDISPATCH_ACTIVATION_COST_DEFAULT_VALUE = 100;
    private static double REDISPATCH_GENERATOR_REQUIRED_MAX_P_DEFAULT_VALUE = 17.5;
    private static double REDISPATCH_COST_UP_DEFAULT_VALUE = 1;
    private static double REDISPATCH_COST_DOWN_DEFAULT_VALUE = 1;
    private static Set<Country> RANGE_ACTIONS_COUNTRIES_DEFAULT_VALUE = ALL_COUNTRIES_SET;

    private static double LIMIT_MULTIPLIER_PREVENTIVE_DEFAULT_VALUE = 1.0;
    private static double LIMIT_MULTIPLIER_OUTAGE_DEFAULT_VALUE = 1.0;
    private static double LIMIT_MULTIPLIER_CURATIVE_DEFAULT_VALUE = 1.0;

    private double affectedCnecMinActivePowerDiff = AFFECTED_CNEC_MIN_ACTIVE_POWER_DIFF_DEFAULT_VALUE;
    private double affectedCnecMinLoadingDiff = AFFECTED_CNEC_MIN_LOADING_DIFF_DEFAULT_VALUE;

    private boolean pstActions = PST_ACTIONS_DEFAULT_VALUE;
    private int pstTapChangeMaxUp = PST_TAP_MAX_CHANGE_UP_DEFAULT_VALUE;
    private int pstTapChangeMaxDown = PST_TAP_MAX_CHANGE_DOWN_DEFAULT_VALUE;

    private boolean redispatchActions = REDISPATCH_ACTIONS_DEFAULT_VALUE;
    private double redispatchGeneratorRequiredMaxP = REDISPATCH_GENERATOR_REQUIRED_MAX_P_DEFAULT_VALUE;
    private double redispatchActivationCost = REDISPATCH_ACTIVATION_COST_DEFAULT_VALUE;
    private double redispatchCostUp = REDISPATCH_COST_UP_DEFAULT_VALUE;
    private double redispatchCostDown = REDISPATCH_COST_DOWN_DEFAULT_VALUE;
    private Set<Country> rangeActionsCountries = RANGE_ACTIONS_COUNTRIES_DEFAULT_VALUE;

    private double limitMultiplierPreventive = LIMIT_MULTIPLIER_PREVENTIVE_DEFAULT_VALUE;
    private double limitMultiplierOutage = LIMIT_MULTIPLIER_OUTAGE_DEFAULT_VALUE;
    private double limitMultiplierCurative = LIMIT_MULTIPLIER_CURATIVE_DEFAULT_VALUE;

}
