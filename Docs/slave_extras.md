## Singularity Extras for Mesos Slaves

*This document assumes you've already followed the steps from [install.md](install.md)*

- `SingularityExecutor`, a custom Mesos executor. This executor provides numerous benefits over the default Mesos executor, including:
  - Thread usage limits
  - Detailed status updates for tasks
  - Log rotation
  - Support for downloading multiple artifacts
  - "Embedded" artifacts (artifacts bundled into the Task object, as opposed to downloaded)
  - Log persistence in S3 (when used in conjunction with `SingularityS3Uploader`)
- `SingularityExecutorCleanup`, a background job that cleans up tasks that haven't cleanly terminated (i.e. have been OOM killed by OS, or the executor experiences a fatal error)
- `SingularityS3Uploader`, a local service that uploads task logs to S3
- `SingularityS3Downloader`, a local service that allows the executor to efficiently download files without being subject to OOM kills due to filling up the page cache.

### 1. Create base property config files.

`/etc/singularity.base.properties` -- properties used by all Singularity slave helpers.

```
# Metadata folder for SingularityS3Uploader
s3uploader.metadata.directory=path/to/s3/metadata

# Metadata folder for SingularityLogWatcher
logwatcher.metadata.directory=path/to/logwatcher/metadata

# Path to write singularity-executor.log, 
root.log.directory=path/to/logs

# Desired logging level
hubspot.log.level=DEBUG
```

---

`/etc/singularity.s3base.properties` -- properties used by jobs that hit S3.

```
# AWS S3 access key
s3.access.key=blah

# AWS S3 secret key
s3.secret.key=secret

# Folder to cache downloaded artifacts in
artifact.cache.directory=path/to/slugs
```

### 2. Install SingularityExecutor

#### 2a. Create a `/etc/singularity.executor.properties` file on each slave:

```
# Folder to store task metadata in (used by SingularityExecutorCleanup)
executor.global.task.definition.directory=path/to/tasks

# Default user to run tasks as
executor.default.user=root

# Folder to store rotated logs
executor.logrotate.to.directory=logs

# AWS S3 bucket name
executor.s3.uplaoder.bucket=bucket-name

# Filename format to use when uploading logs to S3
executor.s3.uploader.pattern=%requestId/%Y/%m/%taskId_%index-%s%fileext

# Additional files to logrotate
executor.logrotate.extras.files=logs/access.log,logs/gc.log

# Path to logrotate command
executor.logrotate.command=/usr/sbin/logrotate

# Whether or not to use the SingularityS3Downloader service to download artifacts
executor.use.local.download.service=true
```

#### 2b. Store the SingularityExecutor JAR in a well-known place on each slave.

#### 2c. Create a shell script to start the executor on each slave.

`/usr/local/bin/singularity-executor`:

```bash
#!/bin/bash

exec java -Djava.library.path=/usr/local/lib -jar path/to/SingularityExecutor-*-shaded.jar
```

### 3. Install SingularityExecutorCleanup (optional)

#### 3a. Create an `/etc/singularity.executor.cleanup.properties` file on each slave.

```
# Path to store executor cleanup results
executor.cleanup.results.directory=path/to/cleanup

# Singularity connection string
singularity.hosts=host:port,host:port

# URL path to singularity API
singularity.context.path=singularity/api
```

#### 3b. Store the SingularityExecutorCleanup JAR in a well-known place on each slave.

#### 3c. Create a shell script to start the executor cleanup job on each slave.

`/usr/local/bin/singularity-executor-cleanup`:

```bash
#!/bin/bash

exec java -jar path/to/SingularityExecutorCleanup-*-shaded.jar
```

#### 3d. Add a crontab entry to run the SingularityExecutorCleanup JAR hourly.

```
0 * * * * root /usr/local/bin/singularity-executor-cleanup > /dev/null 2>&1
```

### 4. Install SingularityS3Downloader (optional)

#### 4a. Store the SingularityS3Downloader JAR in a well-known place on each slave.

#### 4b. Create a shell script to start the S3 downloader service on each slave.

`/usr/local/bin/singularity-s3-downloader`:

```bash
#!/bin/bash

exec java -jar path/to/SingularityS3Downloader-*-shaded.jar
```

#### 4c. Start the SingularityS3Downloader service.

Consider using a tool like monit, supervisor, or systemd to ensure the service stays running.

### 5. Install SingularityS3Uploader (optional)

#### 5a. Create an `/etc/singularity.s3uploader.properties` file on each slave.

**Note**: This file only needed if you need to override the access/secret keys from `singularity.s3base.properties`.

```
# AWS S3 access key
s3.access.key=blah

# AWS S3 secret key
s3.secret.key=secret
```

#### 5b. Store the SingularityS3Uploader JAR in a well-known place on each slave.

#### 5c. Create a shell script to start the S3 uploader service on each slave.

`/usr/local/bin/singularity-s3-uploader`:

```bash
#!/bin/bash

exec java -jar path/to/SingularityS3Uploader-*-shaded.jar
```

#### 5d. Start the SingularityS3Uploader service.

Consider using a tool like monit, supervisor, or systemd to ensure the service stays running.
