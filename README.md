# 📦 TopRAO

Remedial action optimization tool for large power grids, using ToOp for topology optimization and PowSyBl Open RAO for linear optimization (redispatch and PSTs). 
In its current state, the project can run a redispatch computation with open-rao on ToOp output files.

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8 or higher

### Installation

The mvn package command generates a shaded jar file. That is standalone and can be run in command line.

Generate the shaded jar :
```bash
mvn package
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

use help for the additional parameters :

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