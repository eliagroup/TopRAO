# 📦 TopRAO

Remedial action optimization tool for large power grids, using ToOp for topology optimization and PowSyBl Open RAO for linear optimization (redispatch and PSTs). 
In its current state, the project can run a redispatch computation with open-rao on ToOp output files.

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

Project Lead – [Elia Fiftyheartz](elia@fiftyhertz.com) or elia(at)fiftyheartz(.)com

Team – [Mike Blumentopf](mike@energytransmission.fr)

---