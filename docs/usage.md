# Usage

After building the shaded jar (read [Quickstart](quickstart.md)), here is how to run TopRAO from the command line.

## Run Open RAO redispatch computation

For details and inputs read [Redispatch computation](redispatch.md).

### Required inputs

- IIDM network file
- N-1 definition JSON file in ToOp format
- Output directory

### Optional inputs

- RAO parameters JSON file path
- ToOp LF results Parquet file path
- CRAC generation parameters JSON file path
- Topological action JSON string

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