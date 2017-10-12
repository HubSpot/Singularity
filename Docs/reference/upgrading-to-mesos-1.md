Upgrading To Mesos 1.x
======================

Singularity is moving to mesos 1.x, as well as moving to use of the new scheduler http api. There are a few points to keep in mind when upgrading from a previous Singularity version:

### Upgrade Order

- Upgrade the mesos masters
- Upgrade the mesos slaves
- Upgrade the scheduler
- Upgrade the Executors

***Note*** In internal testing we have found the default configuration for older version of the custom executor (pre-mesos-1.x) will work out of the box with newer 1.x mesos, but not vice versa. For this reason, we have recommended to upgrade the executor last so the 1.x executor runs on a 1.x mesos instance

### Configuration Changes

Only a single configuration change is required for SingularityService. The `mesos.master` field in the configuration yaml was previously a zk connection string which allowed Singularity to locate a mesos master. This field is now a comma separated list of mesos master `host:port`. For example:

Before:
```
mesos:
  master: zk://localhost:2181/mesos/my_cluster
```

After:
```
mesos:
  master: my-mesos-master-host-1:5050,my-mesos-master-host-2:5050
```

### Recommended Version and Future Version Bumps

We have targeted a move to mesos 1.1.2 To being with. In versions 1.2 and beyond, the mesos master will no longer accept calls from 0.x frameworks. With Singularity previously built on 0.28, we wanted to give a smoother upgrade path for users where the older scheduler or slaves/agents could still temporarily be registered with the newer mesos master.

The SingularityService module now uses [mesos-rxjava](https://github.com/mesosphere/mesos-rxjava) for communication with the mesos master. This library is meant to be compatible with all mesos 1.x versions. In future releases, the executor will also be modified to use this library. So, mesos version bumps i the 1.x family will be much easier to roll out due to the fact that no nativelibrary bindings will be required and the only version we will be updating will be the mesos package containing the protobufs.

### Other Gotchas For Upgrading

There are a few more flags and deprecated fields to be aware of when upgrading to mesos 1.x

- `--work_dir` flag for slaves/agents is now required. The process will not start without it set
- Internally in Singularity we have started the Slave -> Agent rename. However all UIs and endpoints still use the previous terminology. We have created a few POJO wrappers for backwards compatibility so any `slaveId` fields written to json stored in zookeeper can still be read into the new `agentId` field and vice versa.
