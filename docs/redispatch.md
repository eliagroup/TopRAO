# Redispatch computation

This module allows running a redispatch computation with Open RAO, based on outputs of a ToOp optimization for a specific
topology.  
It will use the network CRAC creator available in Open RAO to build the inputs needed for the optimization.
The CRAC creation can be customized with the [CRAC generation parameters](#crac-generation-parameters), which is a
specific input of TopRAO's redispatch computation.

## Inputs

### IIDM network file

Grid model of PowSyBl. See [IIDM documentation](https://powsybl.readthedocs.io/projects/powsybl-core/en/stable/grid_model/index.html).

### N-1 definition

Definition of contingencies and monitored elements used in ToOp.  
Only the IDs are needed for the elements for TopRAO redispatch.
Here is a minimal example of an N-1 definition file :

```json
{
  "monitored_elements": [
    {
      "id": "line1"
    },
    {
      "id": "line2"
    }
  ],
  "contingencies": [
    {
      "id": "BASECASE",
      "name": "BASECASE",
      "elements": []
    },
    {
      "id": "co_line1",
      "name": "Contingency on Line 1",
      "elements": [
        {
          "id": "line1"
        }
      ]
    }
  ]
}
```

ToOp's N-1 definition format has additional fields, but they are not mandatory and not used for redispatch computation 
in TopRAO.

### CRAC generation parameters

See [CRAC generation parameters](crac_generation_params.md)

### ToOp LF results

List of LF results for monitored branches for all the contingencies. If this input is absent, the module will recompute
a security analysis on the grid for the contingencies and monitored elements specified in the N-1 definition.

There are two possible formats for the file :
- Parquet
columns: `element` (binary, utf8), `contingency` (binary, utf8), `side` (int64), `loading` (double), `p` (double).

- JSON
Example:
```json
[ {
  "element" : "line1",
  "contingency" : "BASECASE",
  "side" : 1,
  "loading" : 0.53,
  "p" : -266.67
}, {
  "element" : "line1",
  "contingency" : "co_line2",
  "side" : 2,
  "loading" : 1.07,
  "p" : -533.33
}]
```

### Topological action

The topological action that is studied. Will be applied to the grid at the beginning of the computation.

Example:
```json
{
  "forced-actions": {
    "preventive-actions-list": {
      "version": 1.3,
      "actions": [
        {
          "type": "SWITCH",
          "id": "Open Switch1",
          "switchId": "Switch1",
          "open": true
        }
      ]
    }
  }
}
```
The forced actions use the powsybl-core action classes,
see [powsybl doc](https://powsybl.readthedocs.io/projects/powsybl-core/en/stable/simulation/security/index.html#remedial-actions)
for the available actions.

### RAO parameters

Parameters for the RAO computation. See [open-rao RAO parameters doc](https://powsybl.readthedocs.io/projects/openrao/en/stable/parameters.html)  

If not specified [default parameters](https://github.com/eliagroup/TopRAO/blob/main/src/main/resources/redispatch/DefaultRaoParameters.json)
are used.
