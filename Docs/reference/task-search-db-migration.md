#### Task Search DB Migration

Before deploying task search (release 0.4.12) it is neccessary to run liquibase migrations `10` and `11`. This migration adds the neccessary columns and indexes, and backfills data for those new columns in the taskHistory table so that searching can be done efficiently and on more fields. If you have a large number of tasks in your database (e.g. more than 100k) , it is possible that these migrations could be very slow when run via liquibase. If this is a concern, we recommend using [pt-online-schema-change](https://www.percona.com/doc/percona-toolkit/2.1/pt-online-schema-change.html) to run your migration.

In order to run your migration with `pt-online-schema-change`, the following command is equal to liquibase migration `10`.

```
pt-online-schema-change \
  --user=(your db user) \
  --ask-pass \
  --alter "ADD KEY startedAt2 (startedAt, requestId), ADD COLUMN host VARCHAR(100) CHARACTER SET ASCII NULL,  ADD COLUMN startedAt TIMESTAMP NULL, DROP KEY deployId, ADD KEY startedAt (requestId, startedAt), ADD KEY lastTaskStatus (requestId, lastTaskStatus, startedAt), ADD KEY deployId (requestId, deployId, startedAt), ADD KEY host (requestId, host, startedAt)" \
  --execute \
  D=(your database name),t=taskHistory
```

This will complete liquibase migration `10`. In order to get the liquibase table in order, you can run a command of `db fast-forward` which will create the entry in the migrations table for the next migration to run. So, if you previously ran migration `9`, it will only create migration `10`.

```
java -jar SingularityService/target/SingularityService-<VERSON>.jar db fast-forward ./config.yaml --migrations migrations.sql
```

The update statements to backfill the newly added columns can also possibly be slow when you have a large number of tasks (e.g. more than 100k) in your history. There are a few options you can use to help this migration run more smoothly:

- Add an additional `ADD KEY host2 (host)` to the end of the `--alter` statement before running the pt-online-schema-change above. This index is not neccessary for search, but will allow the `host` field backfill to run much quicker
- Run each of the update statements in a loop with an additional `LIMIT XXXX` added, where `XXXX` is some number (for example 5000). This way you are not trying to update the entire table in a single query (which would lock the table until the query was done), but are updating it in chunks. You can continue running this loop until the migrations are done.

If you ran the update for migration `11` manually, you can run the `db fast-forward` command from above again in order to update the migrations table for liquibase.

As a last note, if there is a gap of time between running these migrations and deploying the new version of Singularity, it is wise to run the backfill queries manually an additional time. If any tasks have persisted to the database between the intial migration run and the time of deploying the new Singularity version, those tasks will not have the `host` and `startedAt` columns filled in.

