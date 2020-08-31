# Exasol Row Level Security 1.1.2, released 2020-08-31

This release corrects a bug when tenant-security and group-security are used in combination and a user has no groups assigned. In this case the adapter created invalid SQL because an empty `IN()` function was used, which is invalid in Exasol.

Now the adapter checks, whether the user has any groups assigned and only creates the `IN()` parts if the user is a member of one or more groups.

## Bug Fixes
 
* #57: Fixed bug in combination of tenant-security and group-security

## Documentation

* #54: Updated link to deployment of adapter scripts in central Virtual Schema user guide
 
## Dependency updates
 
* Updated `org.itsallcode:openfasttrace-maven-plugin` from 0.1.0 to 1.0.0
