## Historical Data

Singularity can optionally persist all task and deployment historical information into a MySQL database. This is useful because Mesos does not necessarily keep state forever, nor does it provide a deploy-focused interface for viewing that state. The Singularity API and web application will return historical information from both ZooKeeper and MySQL. Singularity will periodically dump stale state into MySQL.

### Configuration

The `database` section of the Singularity configuration file must be populated in order for Singularity to persist task and deploy history information. Here's an example:

```
database:
  driverClass: com.mysql.jdbc.Driver
  user: USERNAME
  password: PASSWORD
  url: jdbc:mysql://HOSTNAME:3306/DB_NAME
```

### Schema Changes

Singularity uses the [dropwizard-migrations](http://dropwizard.io/manual/migrations) bundle (which in turn uses [liquibase](http://www.liquibase.org/)) for managing and applying database schema changes.

To check the status of your database, run the `db status` task:

```
java -jar SingularityService/target/SingularityService-<VERSON>.jar db status ./config.yaml --migrations migrations.sql
INFO  [2013-12-23 18:41:33,620] liquibase: Reading from singularity.DATABASECHANGELOG
INFO  [2013-12-23 18:41:33,668] liquibase: Reading from singularity.DATABASECHANGELOG
1 change sets have not been applied to root@localhost@jdbc:mysql://localhost:3306/singularity
```

To apply pending migrations, run the `db migrate` task:

```
java -jar SingularityService/target/SingularityService-<VERSION>.jar db migrate ./config.yaml --migrations migrations.sql
INFO  [2013-12-23 18:42:08,469] liquibase: Successfully acquired change log lock
INFO  [2013-12-23 18:42:10,206] liquibase: Creating database history table with name: singularity.DATABASECHANGELOG
INFO  [2013-12-23 18:42:10,237] liquibase: Reading from singularity.DATABASECHANGELOG
INFO  [2013-12-23 18:42:10,239] liquibase: Reading from singularity.DATABASECHANGELOG
INFO  [2013-12-23 18:42:10,327] liquibase: ChangeSet migrations.sql::1::tpetr ran successfully in 57ms
```

More information about `db` tasks can be found in the dropwizard-migrations [docs](http://dropwizard.io/manual/migrations), and more information about the migration file syntax can be found in the liquibase [docs](http://www.liquibase.org/documentation/yaml_format.html).

### Purging Old Tasks

You can optionally purge old task data from the db by specifying `historyPurging` configuration. In the configuration for SingularityService, you can have a section similar to the following (default values shown):

```
historyPurging:
  deleteTaskHistoryAfterDays: 365                  # purge tasks older than this
  deleteTaskHistoryAfterTasksPerRequest: 1000      # How many tasks per request before purge
  deleteTaskHistoryBytesInsteadOfEntireRow: true   # Keep the row, just delete data to save space
  checkTaskHistoryEveryHours: 24                   # how often to check for tasks to purge
  enabled: false                                   # determines if we should run the purge
```
