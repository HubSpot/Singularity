## What is Singularity
**Singularity** is a platform that enables deploying and running services and scheduled jobs in cloud infrastructures, providing efficient management of the underlying processes life cycle and effective use of the cluster resources.

![HubSpot PaaS](Docs/images/HubSpot_PaaS.png)

Singularity can be an essential part of a **continuous deployment** infrastructure and is ideal for deploying micro-services. It is optimized to manage thousands of concurrently running processes in hundreds of servers and provides out of the box: 
- a rich REST API for deploying as well as getting information on active and historical deploys and their underlying processes 
- a [web app client](Docs/SingularityUIForDevelopers.md) (Singularity UI) that uses the API to display user friendly views to all available information
- automatic rollback of failing deploys
- automatic fail over of services when the service itself or the server fails 
- automatic cool-down of repeatedly failing services
- health checking at the process and the service endpoint level
- load balancing of multi-instance services
- log rotation and archiving
- resource limits & resource isolation per service instance and graceful killing of instances that exceed their limits
- Rack / availability zone awareness for highly available deploys 

Singularity is a core component of HubSpot PaaS infrastructure allowing us to run thousands of concurrent services and has already executed many millions of tasks in our production and QA clusters.

## How it works
Singularity is an [**Apache Mesos framework**](http://mesos.apache.org/documentation/latest/mesos-frameworks/). It runs as a *task scheduler* on top of **Mesos Clusters** taking advantage of Apache Mesos scalability, fault-tolerance, and resource isolation. [Apache Mesos](http://mesos.apache.org/documentation/latest/mesos-architecture/) is a cluster manager that simplifies the complexity of running different types of applications on a shared pool of servers. In Mesos terminology, *Mesos applications* that are using Mesos APIs to schedule tasks in a cluster are called [*frameworks*](http://mesos.apache.org/documentation/latest/app-framework-development-guide/).

![Mesos Frameworks](Docs/images/Mesos_Frameworks.png)

As the drawing depicts there are different types of frameworks and most frameworks concentrate in supporting a specific type of processing task (e.g. long-running vs scheduled cron-type jobs) or supporting a specific domain and relevant technology (e.g. data processing with hadoop jobs vs data processing with spark). 

Singularity tries to be more generic supporting in one framework many of the common process types that developers need to deploy every day. While mesos allows multiple frameworks to run in parallel, it is important and greatly simplifies the PaaS architecture having a consistent and uniform set of abstractions and APIs for handling deployments across the organization. This was one of the main reasons for HubSpot engineers to initiate the development of a new framework. As of this moment, Singularity supports the following process types:  
- **Web Services**. These are long running processes which expose an API and may run with multiple load balanced instances. Singularity supports automatic configurable health checking of the instances at the process and API endpoint level as well as load balancing. Singularity will automatically restart this type of tasks when they fail or exit and cool down them for a while when they repeatedly fail. 
- **Workers**. These are long running processes, similar to web services, but do not expose an API. *Queue consumers* is common type of worker processes. Singularity dows automatic health checking, cool-down and restart of worker instances.
- **Scheduled (CRON-type) Jobs**. These are tasks that periodically run according to a provided CRON schedule. Scheduled jobs will not be restarted when they fail. Singularity will run them again on the next scheduling cycle. There is provision for retries when starting a scheduled job. Check [this discussion document](Docs/ScheduledJobs) on current limitations and future directions in handling scheduled jobs.
- **On-Demand Processes**. These are manually run processes that will be deployed and be ready to run but singularity will not automatically run them. Users can start them through an API call or using the Singularity Web app.

## Singularity Components
Mesos frameworks have two major components. A **scheduler component** that registers with the **mesos master** to be offered resources and an **executor** component that is launched on cluster slave nodes to run the framework tasks. 

The *mesos master* determines how many resources are offered to each framework and the framework scheduler select which of the offered resources to use to run the required tasks. Mesos slaves do not directly run the tasks but delegate the running to the appropriate executor that has knowledge about the nature of the allocated task and the special handling that might be required.

![Singularity Components](Docs/images/Singularity_Framework_Components.png)

As depicted in the figure, Singularity implements the two basic framework components as well as a few more to solve common complex / tedious problems such as log rotation and archiving without requiring developers to implement it for each task they want to run:

### Singularity Scheduler
The scheduler component implements the required functionality for registering with a mesos master and accepting resource offers and at the same time is a web service offering a rich REST API for accepting requests for different deployable items (web service, worker, etc,) and executing deploys for them by allocating resources and creating / managing the required tasks in the mesos cluster. 

Failed deploy rollback, health checking, load balancing and log rotation configuration setup of service instances are all part of the Singularity Scheduler functionality.

Singularity scheduler uses ZooKeeper as a distributed replication log to maintain state and keep track of registered deployable items, the active deploys for these items and the running tasks that fulfill the deploys. As shown in the drawing, the same Zookeeper quorum utilized by mesos masters and slaves is reused for singularity.  

Since Zoopkeeper is not meant to handle large quantities of data, Singularity utilizes MySQL database to periodically offload metadata from Zookeeper and keep historical records of deployable item changes, deploy request history as well as the history of all launched tasks. 

In production environments singularity is run in high-availability mode by running multiple instances of the Singularity Scheduler component. As depicted only one instance is always active with all the other instances waiting in stand-by mode. While only one instance is registered for receiving resource offers, all instances can process API requests. Singularity uses Zookeeper to perform leader election and maintain a single leader. Because of the ability for all instances to change state, Singularity uses queues which are consumed by the Singularity leader to effect changes in Mesos. 

### Singularity Executor
The executor component main role is to receive the task configuration settings through the mesos slave process (command to execute, environment variables, executable artifact URLs, application configuration files, etc.), setup the execution environment and run the commands that correspond to a mesos task which in turn corresponds to an instance of a deployable item managed by Singularity.

Besides the above basic functionality, Singularity executor offers some advanced features:
- Download and extract **External Artifacts**. Given the URL of an executable artifact it downloads and extracts the artifacts in the tasks sandbox.
### Log Watcher
### S3 uploader
### Executor Cleanup
### OOM Killer
 

## Singularity Abstractions & API
Singularity provides a *deploy oriented* layer on top of Mesos tasks with its **Singularity Request** and **Singularity Deploy** abstractions. An Singularity Request consists of 



