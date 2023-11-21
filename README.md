# Row Level Security

<img alt="row-level-security logo" src="doc/images/row-level-security_128x128.png" style="float:left; padding:0px 10px 10px 10px;"/>

Protect access to database tables on a per-row level based on roles and / or tenants.

## &#9888; Deprecation Warning &#9888;

Please note that the Java-variant of Row-Level-Security is superseded by an [Lua RLS](https://github.com/exasol/row-level-security-lua). The Lua variant is faster and easier to install. It requires Exasol 7.1 or later though.

**Java RLS is discontinued** with the [end of support for Exasol 7.0 on December 31st 2022](https://www.exasol.com/portal/display/DOWNLOAD/Exasol+Life+Cycle). The Lua variant is interface-compatible and can be used as a drop-in replacement.

## Features

Restrict access to rows (datasets) in tables to &hellip;

* set of roles
* tenants (owners)
* group of users
* combination of tenant and role
* combination of tenant and group

## Table of Contents

### Information for Users

* [User Guide](doc/user_guide/user_guide.md)
* [Tutorial](doc/user_guide/tutorial.md)
* [Changelog](doc/changes/changelog.md)
* [Dependencies](dependencies.md)

### Information for Contributors

Requirement, design documents and coverage tags are written in [OpenFastTrace](https://github.com/itsallcode/openfasttrace) format.

* [System Requirement Specification](doc/system_requirements.md)
* [Design](doc/design.md)
