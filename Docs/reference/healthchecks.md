# Health Checks

Health checks in Singularity are divided into two phases and have several parameters that can control the timing and count of checks. A few general options apply to all checks:

- `uri` - the path to hit for healthchecks (e.g. `/status`)
- `portIndex` - Optionally override the index of the dynamically allocated ports to use. By default Singularity will run health checks against the first dynamically allocated port
- `portNumber` - An additonal override to run health checks against a specific port rather than one of the dynamically allocated ones


The first phase is the `startup` phase, where the task being checked is not yet responding to any health checks (as determined by a connection refused). You can set options related to the `startup` phase within the `healthcheck` field on the Singularity deploy:

- `startupTimeoutSeconds` - total time allowed for a task to start responding to healthchecks
- `startupDelaySeconds` - A set amount of time to wait before attempting any health checks
- `startupIntervalSeconds` - The period to wait between checks during the startup phase

After a task has started responding to health checks (with any status code), the following options apply:

- `responseTimeoutSeconds` - The http client's timeout when waiting for a response to the health check
- `intervalSeconds` - The period to wait between checks
- `maxRetries` - The maximum number of time a check will be retried while attempting to get a successful status code in the response (i.e. `maxRetries` of 4 will give a maximum of 5 total checks)
- `failureStatusCodes` - Status codes after which the checks are immediately considered failed, regardless of timeout or retries
