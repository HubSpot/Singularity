## Installing Singularity

*If you just want to test out Singularity, consider using our [docker](../development/developing-with-docker.md) setup instead.*

### 1. Set up a Zookeeper cluster

Singularity uses Zookeeper as its primary datastore -- it cannot run without it.

Chef recipe: [https://supermarket.chef.io/cookbooks/zookeeper](https://supermarket.chef.io/cookbooks/zookeeper)

Puppet module: [https://forge.puppetlabs.com/deric/zookeeper](https://forge.puppetlabs.com/deric/zookeeper)

More info on how to manually set up a Zookeeper cluster lives [here](https://zookeeper.apache.org/doc/r3.3.3/zookeeperAdmin.html#sc_zkMulitServerSetup)

For testing or local development purposes, a single-node cluster running on your local machine is fine. If using the [docker testing/development setup](../development/developing-with-docker.md), this will already be present.

### 2. Set up MySQL (optional)

Singularity can be configured to move stale data from Zookeeper to MySQL after a configurable amount of time, which helps reduce strain on the cluster. If you're running Singularity in Production environment, MySQL is encouraged. See the [database reference](../reference/database.md) for help configuring the database.

### 3. Set up a Mesos cluster

Mesosphere provides a good tutorial for setting up a Mesos cluster: [https://open.mesosphere.com/getting-started/install/](https://open.mesosphere.com/getting-started/install/). You can skip the section on setting up Marathon since Singularity will be our framework instead.

### 4. Build or download the Singularity JAR

In order to run Singularity, you can either build it from scratch or download a precompiled JAR for the `SingularityService` module.

#### Building from Source

Run `mvn clean package` in the root of the Singularity repository. The SingularityService JAR will be created in `SingularityService/target/`.

#### Downloading a precompiled JAR

Singularity JARs are published to Maven Central for each release. You can view the list of SingularityService (the executable piece of Singularity) JARs [here](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.hubspot%22%20AND%20a%3A%22SingularityService%22)

Be sure to only use the `shaded.jar` links -- the other JARs won't work.

### 5. Create a Singularity Config File

Singularity requires a YAML file with some configuration values in order to start up. Here's an example:

```yaml

# Run SingularityService on port 7099 and log to /var/log/singularity-access.log
server:
  type: simple
  applicationContextPath: /singularity
  connector:
    type: http
    port: 7099
  requestLog:
    appenders:
      - type: file
        currentLogFilename: /var/log/singularity-access.log
        archivedLogFilenamePattern: /var/log/singularity-access-%d.log.gz

# omit this entirely if not using MySQL
database: 
  driverClass: com.mysql.jdbc.Driver
  user: [database username]
  password: [database password]
  url: jdbc:mysql://[database host]:[database port]/[database name]

mesos:
  master: http://[mesos master hostname]/api/v1/scheduler
  defaultCpus: 1
  defaultMemory: 128
  frameworkName: Singularity
  frameworkId: Singularity
  frameworkFailoverTimeout: 1000000

zookeeper:
  quorum: [comma separated host:port list of ZK hosts]
  zkNamespace: singularity
  sessionTimeoutMillis: 60000
  connectTimeoutMillis: 5000
  retryBaseSleepTimeMilliseconds: 1000
  retryMaxTries: 3

logging:
  loggers:
    "com.hubspot.singularity" : TRACE

enableCorsFilter: true
sandboxDefaultsToTaskId: false  # enable if using SingularityExecutor

ui:
  title: Singularity (local)
  baseUrl: http://localhost:7099/singularity # If hosting singularity on another domain, use that domain instead of localhost
```

Full configuration documentation lives here: [configuration.md](../reference/configuration.md)

### 6. Run MySQL migrations (if necessary)

If you're operating Singularity with MySQL, you first need to run a liquibase migration to create all appropriate tables: (this snippet assumes your Singularity configuration YAML exists as `singularity_config.yaml`)

`java -jar SingularityService/target/SingularityService-*-shaded.jar db migrate singularity_config.yaml --migrations mysql/migrations.sql`

It's a good idea to run a migration each time you upgrade to a new version of Singularity.

### 7. Start Singularity

`java -jar SingularityService/target/SingularityService-*-shaded.jar server singularity_config.yaml`

**Warning:** Singularity will abort (i.e. exit) when it hits an unrecoverable error, so it's a good idea to use monit, supervisor, or systemd to monitor and automatically restart the Singularity process. It does this in order to simplify the handling of state which must be kept in sync with the Mesos master.

Once started the Singularity UI will be available at http://(host):7099/singularity

### 8. Install extra Singularity tools on Mesos slaves (optional)

Singularity ships with a custom Mesos executor and extra background jobs to make running tasks easier. You can find more info about the slave extras [here](../reference/slave-extras.md).
