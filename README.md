# Singularity

Scheduler for running mesos tasks - long running processes, one-off tasks, and scheduled jobs.

## MVN Deployment process

Prereqs
- sonatype server passwords in ~/.m2/settings.xml for mvn deploy (otherwise, 401)
- access to the sonatype user/password when browsing to the web ui
- gpg installed, key generated and shipped, and passphrase available 

Steps
- Increment all pom versions
- mvn deploy
- In https://oss.sonatype.org/index.html#stagingRepositories, find the release and close it.
- If close succeeds, Release. 
