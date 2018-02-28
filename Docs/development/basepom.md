# Building Singularity on basepom

[Basepom](https://github.com/basepom) is a standardized way to set up
a Maven build. Singularity leverages this to simplify its build.

This is a very basic primer on the features that Singularity uses from
basepom. It should help getting up to speed with the build system.

## Properties

Singularity uses two sets of properties to modify basepom behavior:
general properties and dependency version properties.

The former influence the way the build works while the latter are used
to control dependency versions. Basepom contains a large, curated
section of managed dependencies to allow for consistent and
reproduceable dependency resolution.

### General properties

This is a quick list of general properties set or modified in the
Singularity build. This is a non-exhaustive list; all other available
settings are kept at default value.

#### `project.build.targetJdk`

Controls the JDK level to which the code is compiled. Singularity uses
*1.8* (JDK 8).

#### `basepom.check.skip-license`

The license checker in the OSS version of basepom enforces the
existence of a license header in all source files. As Singularity does
not do this (yet), the checker is disabled by setting this property to
*true*.

#### `basepom.check.fail-extended`

Setting this properties to *`true`* ensures that the extended build
checkers:

* findbugs
* PMD
* checkstyle
* license

will fail the build when they find problems. This switch can be set to
*`false`* using `-Dbasepom.check.fail-extended=false` on the command
line to have the build succeed even if problems are present.

### Dependency properties

The set of curated dependencies in the basepom POMs are generally
suitable for new projects. It may be necessary to override some
dependency versions when converting legacy codes or when a third-party
library requires a fixed version of a dependency.

For Singularity, dropwizard 1.0.x enforces the following versions:

* `dep.jackson.version`, `dep.jackson.core.version`, `dep.jackson.databind.version` - Enforces *2.7.9*, because dw uses Jackson 2.3.x.
* `dep.jetty.version` - Enforces *9.3.9.v20160517* because dw uses Jetty 9.3.x.

## Notes on dependencies

The basepom build uses the maven enforcer plugin to ensure that some
known bad dependencies do not get pulled into a project build. It also
uses the maven duplicate-finder plugin to ensure that no duplicate
resources and classes are present on the class path.

Specifically, the following dependencies are forbidden by the enforcer
plugin:

* `org.slf4j:slf4j-...` - clashes with logback, leads to the dreaded 'Multiple bindings were found on the class path' error
* `org.eclipse.jetty.orbit:javax.servlet` - clashes with the official javax.servlet artifact, leads to duplicate classes
* `com.google.code.findbugs:jsr305` - clashes with com.google.code.findbugs:annotations, leads to duplicate classes

All dependencies that pull in one of these deps have exclusion rules
in the main pom of the project.

### Root and project POM dependencies

As a best practice, none of the actual project poms has any <version>
tags in their dependencies. Instead, maven will resolve these versions
from the project root pom (the one in the root folder), or, in the
case of a well known dependency such as Google Guava, from the basepom
POM. No project pom should ever contain any `<version>` tag (except the
one in the <parent> tag at the top of the project).

### Dependency enforcement and artifact scope

Basepom uses the maven dependency plugin to enforce that all required
dependencies are listed in the project POM. This ensures that

* No dependency required in code for a project is defined "implicitly" (by introduced as a transitive dependency)
* No "dangling" dependencies are left in a project (an artifact listed in the project is not actually used by the project itself and could be removed).

Each of these problems would fail the build.

However, this requires that dependencies need to be correctly
scoped. As an example, the SingularityService uses the
`io.dropwizard:dropwizard-views-mustache` to render its views. This
renderer is discovered at runtime through the Java services lookup
mechanism and is not referenced in code. Therefore, as the dependency
is required at *runtime* but not at *compile* time, it needs to be
scoped in `runtime` scope by adding `<scope>runtime</scope>` to the
dependency.

## Standalone applications

Many of the Singularity sub-projects actually yield standalone
binaries. This gets triggered by the presence of a `.build-executable`
file in the project folder.

Any project that has this file, needs to have a property
`basepom.shaded.main-class` defined in the project POM. E.g. for the
SingularityService there is

```xml
<properties>
  <basepom.shaded.main-class>com.hubspot.singularity.SingularityService</basepom.shaded.main-class>
</properties>
```

defined in the POM for SingularityService.

The build for the project will now yield an additional artifact,
called `SingularityService-...-shaded.jar` which is a standalone
version of the project jar, and an actually binary,
`SingularityService` which can be run from the command line:

```
SingularityService/target % ./SingularityService
usage: java -jar project.jar [-h] [-v] {server,check,db} ...

positional arguments:
  {server,check,db}      available commands

optional arguments:
  -h, --help             show this help message and exit
  -v, --version          show the application version and exit
```
