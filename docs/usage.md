# Usage

After building the shaded jar, run TopRAO from the command line.

## Run open-rao redispatch computation

For details and inpts description see [Redispatch computation](redispatch.md)

### Required inputs

- IIDM network file
- N-1 definition JSON file in ToOp format
- Output directory

### Optional inputs

- Rao parameters json file path
- ToOp LF results parquet file path
- Crac generation parameters files path
- Topology action string

### Example

```bash
java -jar TopRao-1.3.1-SNAPSHOT-shaded.jar -g grid.xiidm -n n1_definition.json -o ./
```

For the full list of command-line options, run:

```bash
java -jar TopRao-1.3.1-SNAPSHOT-shaded.jar --help
```

## Run ToOp topology optimization

Not implemented yet

## Run full ToOp-Open RAO pipeline

Not implemented yet