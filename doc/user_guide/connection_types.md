## Connection Types

RLS only works with Exasol as data source. But you have some options for the connection type to the Exasol instance that you want to protect.

Before we dive into the options, you should first know that for RLS we actually need _two_ database connections. One is for RLS itself and is used to read the metadata like database structure from the source. The other is used for the actual _import_ of payload data.

The one RLS uses is _always_ a JDBC connection. For the other one you can choose.

### Data Source a Remote Exasol Instance or Cluster

Add the following parameters to `CREATE VIRTUAL SCHEMA`:

    IMPORT_FROM_EXA = 'true'
    EXA_CONNECTION_STRING = '<host-or-range>:<port>'

As mentioned before you additionally need to provide a named connection with JDBC details so that RLS can read the metadata.

### Data Source is the Same Exasol Instance or Cluster RLS Runs on

In this case the best possible connection type is a so called "local" connection.

Add the following parameters to `CREATE VIRTUAL SCHEMA`:

    IS_LOCAL = 'true'

The parameter `IS_LOCAL` provides an additional speed-up in this particular use case. The way this works is that RLS generates a regular `SELECT` statement instead of an `IMPORT` statement. And that `SELECT` can be directly executed by the core database, whereas the `IMPORT` statement takes a detour via the ExaLoader.

That means whenever you are protecting data on the same instance or cluster, you definitely should use `IS_LOCAL`!

### Data Source is an Exasol Instance or Cluster Only Reachable via JDBC

While this connection type works, it is also the slowest option and exists mainly to support integration tests on the ExaLoader. We recommend that you use `IMPORT FROM EXA` instead.