# 📦 TopRAO

Remedial action optimization tool for large power grids, aiming to provide fast and complete results using two power grid optimization tools :

- [ToOp](https://github.com/eliagroup/toop)  
Open-source topological remedial actions optimization tool.  

- [PowSyBl Open RAO](https://github.com/powsybl/powsybl-open-rao)  
Open-source toolbox providing a modular engine for remedial actions optimization, used in TopRAO for linearizable actions (PST tap changes and redispatch on generators).

The aim of the project is to provide 3 possible computations in Java or in command line :

- Run a topology optimization on an IIDM grid file using ToOp.
- Run a redispatch and PST actions optimization based on the outputs of a ToOp run for a specific topology using OpenRAO.
- Run a full pipeline with topology, redispatch and PST optimization with the selection of the most promising topologies.

In the current state, only the second computation is implemented.

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8 or higher

### Installation

Currently, this project uses a version of open-rao that is still in development : https://github.com/powsybl/powsybl-open-rao/tree/feature/cnec-creator-branch-names

To install the project, you need to checkout that branch and build it locally.
```bash
cd path/to/openrao_parent # choose a folder for open-rao code
git clone --branch feature/cnec-creator-branch-names --single-branch https://github.com/powsybl/powsybl-open-rao.git
cd powsybl-open-rao
mvn install
```

---

## Usage

The mvn package command generates a shaded jar file, which is portable and standalone.

Generate the shaded jar from the TopRAO root dir:
```bash
mvn package
```

Running a Maven build also configures the repository to use the versioned Git hooks from .githooks/.
You can install the Git hook configuration explicitly with:
```bash
mvn git-build-hook:configure
```

---

## Usage

Once the shaded jar is generated it can be run in command line.

The requested inputs are :
- IIDM network file
- n-1 definition json file in ToOp format
- path for output files

```bash
java -jar TopRao-1.3.1-SNAPSHOT-shaded.jar -g grid.xiidm -n n1_definition.json -o ./
```

Use help for the additional parameters :

```bash
java -jar TopRao-1.3.1-SNAPSHOT-shaded.jar --help
```

---


## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md)

---

## License

See [LICENSE](LICENSE)

---

## Contact
Team – [ToOp](mailto:ToOp@eliagroup.eu)

If you require help with using this package, your first point of contact is <a href="mailto:ToOp@eliagroup.eu">ToOp@eliagroup.eu</a>.

---
