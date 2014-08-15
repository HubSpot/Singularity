# Singularity

Scheduler (HTTP API and web interface) for running [mesos](http://mesos.apache.org/) tasks - long running processes, one-off tasks, and scheduled jobs.

## Overview

Singularity is used by HubSpot to run hundreds of concurrent services and has executed many millions of tasks. It provides the ability to quickly and reliably run thousands of tasks inside of a Mesos cluster and provides easy access to them through a modern web interface. Singularity will automatically reschedule tasks that are killed or lost; it can mitigate hardware failures and allow slaves to be gracefully decommissioned. It supports the concept of deployments and healthchecks and will automatically rollback bad deploys. It can integrate with a load balancer API like [Baragon](https://github.com/HubSpot/Baragon) to coordinate web service deployments. Singularity provides a Mesos executor which supports additional features like downloading and caching of build files from S3, and tailing, archiving, and cleaning of service logs.

## How it works

You make a Request to Singularity to define the type of task that you would like to run (cron schedule, # of task instances, rack awareness, etc.) You then post a Deploy to that Request with the command or executor data about how to run that task, and a variety of optional configuration options (like cpu, memory resources or healthcheck urls.) Singularity registers with a Mesos master and receives resource offers from Mesos and attempts to match those offers (which include resources as well as rack information and what else is running on that Slave) with its list of Requests that have yet to be fulfilled. Once an offer is accepted by Singularity, a task is launched inside of Mesos on the Slave which the offer referenced. Singularity maintains its own state about Requests and Deploys and the Mesos tasks which fulfill them.

[Overview, Features, and Design](overview.md)

## Requirements

- Mesos
- ZooKeeper
- MySQL

## Installation & Deployment

- [Local development using Vagrant](vagrant.md)
- [Database configuration and schema](database.md)
- Singularity configuration

## Operation & Documentation

- [API Endpoints](SingularityService/api.md)
- [API Objects](SingularityService/objects.md)
- Suggested practices (monitoring & HA)

## Developer Info

- [Scheduled Execution](scheduled.md)
- [Maven deployment](maven.md)
