package com.toprao.crac;

import com.powsybl.iidm.network.Country;
import com.toprao.JsonUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
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

    public static CracGenerationParameters read(Path filePath) {
        try (InputStream is = Files.newInputStream(filePath)) {
            return readFromInputStream(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static CracGenerationParameters readFromInputStream(InputStream is) {
        Objects.requireNonNull(is);
        try {
            return JsonUtils.getObjectMapper().readValue(is, CracGenerationParameters.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void write(Path p) {
        try {
            JsonUtils.getObjectMapper().writeValue(p.toFile(), this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
