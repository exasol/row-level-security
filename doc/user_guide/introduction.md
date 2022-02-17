## Introduction

### Row-Level Security Works on a Per-Schema Level

As mentioned before, RLS is a specialized implementation of a [Virtual Schema](https://github.com/exasol/virtual-schemas). As the name suggests a database schema is the scope that can be protected. That means if you want to protect multiple schemas, you need to configure multiple RLS Virtual Schemas on top of them.

### RLS Variants

RLS comes in three flavors which can also be used in combination:

1. Tenant-based security
1. Group-based security
1. Role-based security

The following subsections explain the variants in detail.

#### Tenant-based Security

Tenant-based security is the simplest of the three mechanisms. Think of the tenant as the person owning a dataset (aka. "row"). In a tenant-protected table, each row belongs to a tenant. Only the tenant gets to see the rows content.

As the name suggests, the main use case is keeping data for multiple tenants in the same location, while still being able to enforce read restrictions.

#### Group-based Security

A group in the RLS sense is a collection of people. Each user can belong to zero or more groups.

As an example take a soccer team and a chess club. You might be a member in one of them, both or none.

Each row can assigned with to _single_ group. In this case you can think of that group collectively owning the row.

If one of the groups a user is a member of matches the group owning a row, that user may read the row's contents.

#### Role-based Security

A role in the real world is a responsibility that comes with certain privileges. A person with the role "lab worker" for example may enter different parts of a building than a person who fulfills the role of "janitor".

Translated to RLS that privilege is reading rows.

You can define up to 63 different RLS roles per Schema. A 64th role is reserved to indicate public access. Note that those are not regular database roles ([more about that in the next section](#rls-roles-are-not-database-roles)) but an RLS-specific concept.

If you introduce role protection on a table, you can assign any combination of roles to each row.

RLS determines read permissions by matching a user's roles with a row's roles. If they overlap in at least one role, the user may view the row.

##### RLS Roles are not Database Roles

Note that when we talk about "roles" in this document, we mean RLS roles, not database roles. [Database roles](https://docs.exasol.com/sql/create_role.htm) are a completely disparate concept. While database roles control permissions on database objects like tables, they must be managed by a database administrator. RLS roles on the other hand are on a higher level and can be managed without database administrator privileges by the owner of the schema that needs to be protected.

This is an important distinction since it allows for separation of concerns. Database administrators are in this scenario responsible for the security of the database as a whole. Schema owners get to decide who sees what in their schema.

##### Roles are not Groups

In a soccer team for example you have the role of a goal keeper, defenders or a coach. All have different responsibilities and different things they are allowed to do. The goal keeper for example is the only player in the game allowed to touch the ball with the hands.

Don't confuse roles with groups. In our example the football team would be a group.

#### Public Access

RLS also allows rows to be defined as public.

The simplest way to make data public is by not applying any of the protection schemes to a table. As an example, think of a dimension table `WEEKDAYS` in a data warehouse. There is really no point in hiding this one. We all know what days a week has anyway.

In role-based security you can mark rows as public by assigning the reserved public role to them. Note that if a row has the public role, all other roles it might also have are irrelevant. Access can't be more permissive than public.

### Protection Scope of RLS

The regular protection mechanisms of Exasol always apply. You can for example control who has access to the Virtual Schema that provides RLS. RLS is an additional mechanism on top of that which offers finer control for data visibility.

RLS protects individual rows inside selected tables. Tables inside an Exasol RLS Virtual Schema can either be RLS-protected or public. In a public table all users who can access the Virtual Schema can see the data inside the table.

On the lowest level RLS protects data per row (aka. dataset). To determine whether or not a user is allowed to read a row, RLS checks the existence and contents of special system columns in that table.