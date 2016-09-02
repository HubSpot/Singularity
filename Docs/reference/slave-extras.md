## Singularity Extras for Mesos Slaves

*This document assumes you've already followed the steps from [install.md](../getting-started/install.md)*

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

`/etc/singularity.base.yaml` -- properties used by all Singularity slave helpers.

```yaml
# Metadata folder for SingularityS3Uploader
s3UploaderMetadataDirectory: path/to/s3/metadata

# Metadata folder for SingularityLogWatcher
logWatcherMetadataDirectory: path/to/logwatcher/metadata

# Path to write singularity-executor.log
loggingDirectory: path/to/logs

# Desired logging level
loggingLevel:
   com.hubspot: INFO
```

---

`/etc/singularity.s3base.yaml` -- properties used by jobs that hit S3.

```yaml
# AWS S3 access key
s3AccessKey: blah

# AWS S3 secret key
s3SecretKey: secret

# Folder to cache downloaded artifacts in
artifactCacheDirectory: path/to/slugs
```

### 2. Install SingularityExecutor

#### 2a. Create a `/etc/singularity.executor.yaml` file on each slave:

```yaml
# Folder to store task metadata in (used by SingularityExecutorCleanup)
globalTaskDefinitionDirectory: path/to/tasks

# Default user to run tasks as
defaultRunAsUser: root

# Folder to store rotated logs
logrotateToDirectory: logs

# AWS S3 bucket name
s3UploaderBucket: bucket-name

# Filename format to use when uploading logs to S3
s3UploaderKeyPattern: "%requestId/%Y/%m/%taskId_%index-%s%fileext"

# Additional files to logrotate
logrotateAdditionalFiles:
- logs/access.log
- logs/gc.log

# Path to logrotate command
logrotateCommand: /usr/sbin/logrotate

# Whether or not to use the SingularityS3Downloader service to download artifacts
useLocalDownloadService: true
```

#### 2b. Store the SingularityExecutor JAR in a well-known place on each slave.

#### 2c. Create a shell script to start the executor on each slave.

`/usr/local/bin/singularity-executor`:

```bash
#!/bin/bash

exec java -Djava.library.path=/usr/local/lib -jar path/to/SingularityExecutor-*-shaded.jar
```

#### 2d. Update Singularity configuration to allocate resources for custom executors.

When launching Mesos tasks with a custom executor (i.e. `SingularityExecutor`), you must allocate additional resources to the executor process. Custom executor resources can be set via the `customExecutor` field:

```yaml
customExecutor:
  memoryMb: 128
  numCpus: 0.1
```

This snippet will make Singularity launch tasks with an additional `128 MB` of memory and `0.1 CPU` devoted solely to the executor.

(This is not necessary if you're not using custom executors -- Mesos will automatically pad your resources to accommodate the default executor.)

### 3. Install SingularityExecutorCleanup (optional)

#### 3a. Create an `/etc/singularity.executor.cleanup.yaml` file on each slave.

```yaml
# Path to store executor cleanup results
executorCleanupResultsDirectory: path/to/cleanup

# Singularity connection string
singularityHosts:
- host:port
- host:port

# URL path to singularity API
singularityContextPath: singularity/api
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

#### 5a. Create an `/etc/singularity.s3uploader.yaml` file on each slave.

**Note**: This file only needed if you need to override the access/secret keys from `singularity.s3base.yaml`.

```yaml
# AWS S3 access key
s3AccessKey: blah

# AWS S3 secret key
s3SecretKey: secret
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
