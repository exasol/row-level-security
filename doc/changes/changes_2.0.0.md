# Exasol Row Level Security 2.0.0, released 2020-11-26

Code name: Fixed credentials exposure in EXA connection

## Summary

Row Level Security is based on the [`exasol-virtual-schema`](https://github.com/exasol/exasol-virtual-schema) and thus
inherited a credential exposure that has been fixed in Exasol VS 4.0.0 and is now fixed in RLS too.

If you used `IMPORT_FROM_EXA` in a previous version, you need to remove the old property EXA_CONNECTION_STRING,
create a named connection definition of type EXA with CREATE CONNECTION and provide the name of that definition in the
new property EXA_CONNECTION.

The old variant is intentionally not supported anymore to tighten security.

## Refactoring

* #6: Updated to the latest version of Exasol dialect containing a security update.

## Dependency updates
 
 * Updated `com.exasol:exasol-virtual-schema:3.1.0` to 4.0.0