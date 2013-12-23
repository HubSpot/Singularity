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

## Database

### Configuration

The `database` section of the Singularity configuration file must be populated in order for Singularity to persist task history information. Here's an example:

```
database:
  driverClass: com.mysql.jdbc.Driver
  user: USERNAME
  password: PASSWORD
  url: jdbc:mysql://HOSTNAME:3306/DB_NAME
```

### Schema changes

Singularity uses the [dropwizard-migrations](http://dropwizard.codahale.com/manual/migrations/) bundle (which in turn uses [liquibase](http://www.liquibase.org/)) for managing and applying database schema changes.

To check the status of your database, run the `db status` task:

```
java -jar SingularityService/target/SingularityService-0.2.12.jar db status ./config.yaml --migrations SingularityService/src/main/resources/migrations.yaml
INFO  [2013-12-23 18:41:33,620] liquibase: Reading from singularity11.DATABASECHANGELOG
INFO  [2013-12-23 18:41:33,668] liquibase: Reading from singularity11.DATABASECHANGELOG
2 change sets have not been applied to root@localhost@jdbc:mysql://localhost:3306/singularity11
```

To apply pending migrations, run the `db migrate` task:

```
java -jar SingularityService/target/SingularityService-0.2.12.jar db migrate ./config.yaml --migrations SingularityService/src/main/resources/migrations.yaml
INFO  [2013-12-23 18:42:08,469] liquibase: Successfully acquired change log lock
INFO  [2013-12-23 18:42:10,206] liquibase: Creating database history table with name: singularity11.DATABASECHANGELOG
INFO  [2013-12-23 18:42:10,237] liquibase: Reading from singularity11.DATABASECHANGELOG
INFO  [2013-12-23 18:42:10,239] liquibase: Reading from singularity11.DATABASECHANGELOG
INFO  [2013-12-23 18:42:10,327] liquibase: ChangeSet SingularityService/src/main/resources/migrations.yaml::1::tpetr ran successfully in 57ms
```

More information about `db` tasks can be found in the dropwizard-migrations [docs](http://dropwizard.codahale.com/manual/migrations/), and more information about the migration file syntax can be found in the liquibase [docs](http://www.liquibase.org/documentation/yaml_format.html).
