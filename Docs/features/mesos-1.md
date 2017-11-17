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

### SingularityClient Considerations

As part of the mesos 1 update, the `org.apache.mesos:mesos` library is now pulling in a newer version of protobuf. This can cause issues for users using any other protobuf version. As a result, we have refactored the models in `SingularityBase` such that `SingularityBase` and the `SingularityClient` no longer have a dependency on `org.apache.mesos:mesos`.

For users of the java client, this means that a few of the previously accessible methods on the `SingularityTask` object may not be present. All information from the mesos `TaskInfo` protos is still being saved as json for later usage, but only the parts needed by Singularity internals are mapped to POJO fields, with the remainder being caught by jackson's `@JsonAnyGetter`/`@JsonAnySetter`. Extra fields on objects are available as a `Map<String, Object>` under `getAllOtherFields` on the objects.

See [#1648](https://github.com/HubSpot/Singularity/pull/1648) for more details.

### Other Mesos Considerations

- The `--work_dir` flag _must_ be set on all mesos agents or they will error on startup
- internally the slave -> agent rename is being used, but all apis endpoints and fields still reference slave as they did before. Singularity will tackle the slave -> agent rename in future versions
