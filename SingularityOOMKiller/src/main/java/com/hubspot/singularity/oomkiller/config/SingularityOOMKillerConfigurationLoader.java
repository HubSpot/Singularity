package com.hubspot.singularity.oomkiller.config;

import java.util.Properties;

import com.hubspot.singularity.runner.base.config.SingularityConfigurationLoader;

public class SingularityOOMKillerConfigurationLoader extends SingularityConfigurationLoader {

  public static final String REQUEST_KILL_THRESHOLD_RATIO = "oomkiller.request.kill.threshold.ratio";
  public static final String KILL_PROCESS_DIRECTLY_THRESHOLD_RATIO = "oomkiller.kill.process.directly.threshold.ratio";
  public static final String CHECK_FOR_OOM_EVERY_MILLIS = "oomkiller.check.for.oom.every.millis";
  public static final String SLAVE_HOSTNAME = "oomkiller.slave.hostname";
  public static final String CGROUP_PROCS_PATH_FORMAT = "oomkiller.cgroups.procs.path.format";

  public SingularityOOMKillerConfigurationLoader() {
    super("/etc/singularity.oomkiller.properties");
  }

  @Override
  public void bindDefaults(Properties properties) {
    properties.put(REQUEST_KILL_THRESHOLD_RATIO, "1.0");
    properties.put(KILL_PROCESS_DIRECTLY_THRESHOLD_RATIO, "1.2");

    properties.put(CHECK_FOR_OOM_EVERY_MILLIS, "100");
    properties.put(SLAVE_HOSTNAME, "localhost");

    properties.put(CGROUP_PROCS_PATH_FORMAT, "/cgroup/cpu/mesos/%s/cgroup.procs");
  }

}
