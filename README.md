# Singularity

Scheduler (HTTP API and web interface) for running [mesos](http://mesos.apache.org/) tasks - long running processes, one-off tasks, and scheduled jobs.

## Overview

Singularity is a scheduler for running long running tasks and scheduled tasks inside of Mesos. You make a Request to Singularity to define the task(s) (which is a scheduled job or long running process) that the Mesos executor should execute and the metadata about how to run these tasks - the # of task instances, the resources (cpus, memory) each task should consume, and optionally a cron schedule, as well as other configuration options. Singularity registers with a Mesos master and receives resource offers from Mesos and attempts to match those offers (which include resources as well as rack information and what else is running on that Slave) with its list of Requests that have yet to be fulfilled. Once an offer is accepted by Singularity, a task is launched inside of Mesos on the Slave which the offer referenced.

[Overview, Features, and Design](overview.md)

## Requirements

- Mesos
- ZooKeeper
- MySQL

## Installation & Deployment

- [Database configuration and schema](database.md)
- Singularity configuration

## Operation & Documentation

- [API Endpoints](SingularityService/api.md)
- [API Objects](SingularityService/objects.md)
- Suggested practices (monitoring & HA)

## Developer Info

- [Scheduled Execution](scheduled.md)
- [Maven deployment](maven.md)
