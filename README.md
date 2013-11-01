# Singularity

Scheduler for running mesos tasks - long running processes, one-off tasks, and scheduled jobs.

## MVN Deployment process

- Increment all pom versions
- mvn deploy
- In https://oss.sonatype.org/index.html#stagingRepositories, find the release and close it.
- If close succeeds, Release.