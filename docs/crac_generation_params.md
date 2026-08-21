# CRAC generation parameters

Parameters used by TopRAO to filter the monitored elements and create remedial actions from the elements present 
in the grid.

## Parameters

### affectedCnecMinActivePowerDiff
A branch is monitored in the RAO for a specific contingency if:

$$
|P_{basecase} - P_{contingency}| > affectedCnecMinActivePowerDiff
$$

Default value is `5` (in MW).

### affectedCnecMinLoadingDiff
A branch is monitored in the RAO for a specific contingency if:

$$
|Loading_{basecase} - Loading_{contingency}| > affectedCnecMinLoadingDiff
$$

Where $Loading_{state}$ is the ratio of the current (or active power) on the branch over the current (or active power) limit on the branch.

Default value is `0.1`

### pstActions
Whether PST actions should be enabled.

Default value is `true`.

### pstTapChangeMaxUp
Maximum taps change allowed for the optimizer in direction "up".

Default value is `3`.

### pstTapChangeMaxDown

Maximum taps change allowed for the optimizer in direction "down".

Default value is `3`.

### redispatchActions
Whether redispatch actions on generators should be enabled.

Default value is `true`.

### redispatchGeneratorRequiredMaxP
Minimal value of generator's maxP required to be considered in redispatch actions (in MW).

Default value is `17.5`

### redispatchActivationCost
Activation cost of a generator.

Default value is `100`

### redispatchCostUp
Cost of 1 MW change up on a generator.

Default value is `1`

### redispatchCostDown
Cost of 1 MW change down on a generator.

Default value is `1`

### rangeActionsCountries
Countries for which PSTs and generators actions are created.

Default value is all countries.

### limitMultiplierPreventive
Multiplier applied on branches' limits for preventive state.

Default value is `1`

### limitMultiplierOutage
Multiplier applied on branches' limits for outage state.

Default value is `1`

### limitMultiplierCurative
Multiplier applied on branches' limits for curative state.

Default value is `1`

## JSON representation
Example :
```json
{
  "redispatch_cost_up": 2,
  "redispatch_cost_down": 2,
  "range_actions_countries": ["DE", "FR"]
}
```

