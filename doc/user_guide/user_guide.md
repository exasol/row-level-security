# Row Level Security

Row-Level Security (short "RLS") is a security mechanism based on Exasol's Virtual Schemas. It allows database administrators control access to a table's row depending on a user's roles and username.

RLS only supports Exasol databases. That means you cannot use RLS between Exasol and a 3rd-party data source.

The RLS installation package contains everything you need to extend an existing Exasol installation with row-level security.

The user guide is divided into the following sections. If you are new to the subject, we recommend reading them in order otherwise you can jump to the part where you need details.

**Table of Contents**

* [Introduction](introduction.md)
* [Administration](administration.md)
* [Connection types](connection_types.md)
* [Limitations](limitations.md)

"Introduction" explains the basic concepts behind RLS. Administration demonstrates how to set up an RLS protected schema. In "Connection types" we talk about the options you have to connect to the source schema. In "Limitations" finally you find a list of technical limitations that you should be aware of when using RLS.