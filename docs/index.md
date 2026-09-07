# TopRAO

Remedial action optimization tool for large power grids, aiming to provide fast and complete results using two power grid optimization tools :

- [ToOp](https://github.com/eliagroup/toop)  
  Open-source topological remedial actions optimization tool.

- [PowSyBl Open RAO](https://github.com/powsybl/powsybl-open-rao)  
  Open-source toolbox providing a modular engine for remedial actions optimization, used in TopRAO for linearizable actions (PST tap changes and redispatch on generators).

The aim of the project is to provide 3 possible computations in Java or with command line :

- Run a topology optimization on an IIDM grid file using ToOp.
- Run a redispatch and PST actions optimization based on the outputs of a ToOp run for a specific topology using OpenRAO.
- Run a full pipeline with topology, redispatch and PST optimization with the selection of the most promising topologies.

In the current state, only the second computation is implemented.
