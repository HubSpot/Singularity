## Overview ##

Singularity is an API and web application for running and scheduling [Apache Mesos](http://mesos.apache.org/) tasks — including long running processes, scheduled jobs, and one-off tasks.

It focuses on a batteries-included approach: Singularity and its components provide an entire Platform as a Service (PaaS) to end-users. It has many features which have been introduced to reduce developer friction and ensure proper operation and reliable deployment of tasks. Users may even be unfamiliar with and shielded from the details of Mesos.

For a more thorough explanation of the concepts behind Singularity and Mesos click [here](Docs/details.md).

----------

### Features###

 - [Native Docker Support](Docs/containers.md)
 - [JSON REST API and Java Client](Docs/api.md)
 - [Rich load balancer integration with Baragon](Docs/baragon.md)
 - [Web Application](Docs/ui.md)
 - [Deployments, automatic rollbacks, and healthchecks](Docs/deployments.md)
 - [Webhooks for third party integrations](Docs/webhooks.md)
 - [Configurable email alerts to service owners](Docs/email.md)
 - [Historical deployment and task data](Docs/database.md)
 - [Custom executor with extended log features](Docs/components.md)

----------

## Getting Started ##
##### Requirements #####

 - [Mesos](http://mesos.apache.org/gettingstarted/)
 - [ZooKeeper](https://zookeeper.apache.org/doc/r3.4.6/zookeeperStarted.html) 
 - Java 7+
 - [MySQL](http://dev.mysql.com/usingmysql/get_started.html) (optional)

##### Contact #####

- [singularity-users@googlegroups.com](mailto:singularity-users@googlegroups.com) // [singularity-users](https://groups.google.com/forum/#!topic/singularity-users/)
- \#singularity-framework on freenode

### Install ###

##### Local Testing #####

----------

## Reference ##

#### Deployment ####

 - [API](Docs/api.md)
 - [Configuration](Docs/configuration.md)
 - [Custom Executor Components](Docs/components.md)

#### Development ####

- [Hacking on the UI](Docs/development/ui.md)
- [Understanding the basepom / Maven structure](Docs/development/basepom.md)
- [Third-party load balancer API design requirements](Docs/development/lbs.md)
- [Publishing releases (for committers)](Docs/development/maven.md)

 