Upgrading Singularity to use Mesos `1.x`
========================================

Starting with release version `0.18.0`, Singularity will use the mesos http api to communicate with the mesos master via the [mesos rx-java library](https://github.com/mesosphere/mesos-rxjava). Documentation on upgrading mesos itself can be found on the [mesos website](http://mesos.apache.org/documentation/latest/upgrades/). In order to upgrade Singularity there are a few things to keep in mind beyond the scope of a normal release:

### Mesos Version Selection

As of mesos `1.2`, the mesos master will no longer accept registrations from mesos agents running `0.x.x` versions. As a result, we have chosen to release Singularity `0.18.0` built against mesos `1.1.2`, allowing for a smoother upgrade path for users.

For future `1.x` version upgrades, less overall change should be needed due to the fact that we are now using the http api and do not depend on native libraires being installed.

### Singularity Executor Updates

If you are running the custom Singularity Executor, we recomend updating mesos on your agents _before_ updating the Singularity Executor. We have found in our testing that the older executor (built against `0.x`) can run smoothly on mesos `1.1`, but the inverse is not always true.

### Singularity Service Configuration Updates

The configuration to connect to the mesos master is the only field that has changed with the 1.x upgrade. The new `mesos.master` field in the configuration yaml is now a comma seaprated list of mesos master `host:port` vaules. Singularity will randomly select from the list when searching for a master (1.x masters will automatically redirect requests to the leading master), trying other hosts in the list if it is not successful.

For example an old configuration of:

```yaml
mesos:
  master: zk://my-zk-hostname.com:2181/mesos/singularity

```

Would now become:

```yaml
mesos:
  master: my-mesos-master-host.com:5050

```

### Other Mesos Considerations

- The `--work_dir` flag _must_ be set on all mesos agents or they will error on startup
- internally the slave -> agent rename is being used, but all apis endpoints and fields still reference slave as they did before. Singularity will tackle the slave -> agent rename in future versions