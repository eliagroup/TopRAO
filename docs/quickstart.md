# Quickstart

## Prerequisites

- Java 21
- Maven 3.8 or higher

## Build

Create the shaded jar with:

```bash
mvn package
```

This also configures the repository to use the versioned Git hooks from `.githooks/`.

To configure the Git hooks explicitly, run:

```bash
mvn git-build-hook:configure
```