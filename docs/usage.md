# Usage

After building the shaded jar, run TopRAO from the command line.

## Required inputs

- IIDM network file
- N-1 definition JSON file in ToOp format
- Output directory

## Example

```bash
java -jar TopRao-1.3.1-SNAPSHOT-shaded.jar -g grid.xiidm -n n1_definition.json -o ./
```

For the full list of command-line options, run:

```bash
java -jar TopRao-1.3.1-SNAPSHOT-shaded.jar --help
```