package com.toprao.crac;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.io.network.parameters.CriticalElements;
import com.powsybl.openrao.data.crac.io.network.parameters.NetworkCracCreationParameters;
import com.powsybl.openrao.data.crac.io.network.parameters.PstRangeActions;
import com.powsybl.openrao.data.crac.io.network.parameters.RangeActionCosts;
import com.toprao.toop.data.ToOpLfResult;
import com.toprao.toop.data.ToOpN1Definition;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class FullPreventiveRaoSpecifier extends AbstractCnecFilteringSpecifier {

    private static final String PREVENTIVE = "preventive";
    private static final String OUTAGE = "outage";

    public FullPreventiveRaoSpecifier(ToOpN1Definition n1Definition, ToOpLfResult toOpLfResult, CracGenerationParameters cracGenerationParameters, Integer cocbLimit) {
        super(n1Definition, toOpLfResult, cracGenerationParameters, cocbLimit);
    }

    @Override
    public void postprocessCrac(Crac crac) {
        // nothing to do
    }

    @Override
    public CracCreationParameters getCracCreationParameters(Network network) {
        log.info("Creating Crac creation parameters");
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        NetworkCracCreationParameters parameters = new NetworkCracCreationParameters(null, List.of());
        cracCreationParameters.addExtension(NetworkCracCreationParameters.class, parameters);

        createCnecParameters(parameters);

        createRaParameters(parameters);

        log.info("Crac creation parameters created");
        return cracCreationParameters;
    }

    private void createCnecParameters(NetworkCracCreationParameters parameters) {
        createFilteredCnecParameters(parameters);
        parameters.getCriticalElements().setThresholdDefinition(CriticalElements.ThresholdDefinition.PERM_LIMIT_MULTIPLIER);
        parameters.getCriticalElements().setLimitMultiplierPerInstant(Map.of(PREVENTIVE, cracGenerationParameters.getLimitMultiplierPreventive(), OUTAGE, cracGenerationParameters.getLimitMultiplierOutage()));
    }

    protected void createRaParameters(NetworkCracCreationParameters parameters) {
        parameters.getCountertradingRangeActions().setCountryFilter(Set.of(Country.AM));

        if (cracGenerationParameters.isPstActions()) {
            parameters.getPstRangeActions().setCountryFilter(cracGenerationParameters.getRangeActionsCountries());
            parameters.getPstRangeActions().setPstRaPredicate((pst, state, context) -> state.isPreventive());
            parameters.getPstRangeActions().setAvailableTapRangesAtInstants(
                    Map.of(
                            PREVENTIVE, new PstRangeActions.TapRange(
                                    -cracGenerationParameters.getPstTapChangeMaxDown(),
                                    cracGenerationParameters.getPstTapChangeMaxUp(),
                                    RangeType.RELATIVE_TO_INITIAL_NETWORK))
            );
        } else {
            parameters.getPstRangeActions().setCountryFilter(Set.of(Country.AM));
        }

        if (cracGenerationParameters.isRedispatchActions()) {
            parameters.getRedispatchingRangeActions().setIncludeAllInjections(true);
            parameters.getRedispatchingRangeActions().setCountryFilter(cracGenerationParameters.getRangeActionsCountries());

            parameters.getRedispatchingRangeActions().setRdRaPredicate((injection, instant, context) -> instant.isPreventive()
                    && injection.getType() == IdentifiableType.GENERATOR
                    && ((Generator) injection).getMaxP() > cracGenerationParameters.getRedispatchGeneratorRequiredMaxP()
                    && ((Generator) injection).getMaxP() < Double.MAX_VALUE
                    && ((Generator) injection).getMinP() > -Double.MAX_VALUE);
            parameters.getRedispatchingRangeActions().setRaCostsProvider((injection, instant) ->
                    new RangeActionCosts(cracGenerationParameters.getRedispatchActivationCost(), cracGenerationParameters.getRedispatchCostUp(), cracGenerationParameters.getRedispatchCostDown()));
        } else {
            parameters.getRedispatchingRangeActions().setIncludeAllInjections(false);
            parameters.getRedispatchingRangeActions().setCountryFilter(Set.of(Country.AM));
        }
    }
}
