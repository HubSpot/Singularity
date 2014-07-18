package com.hubspot.singularity.oomkiller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.client.SingularityClient;
import com.hubspot.singularity.oomkiller.config.SingularityOOMKillerConfiguration;

public class SingularityOOMKiller {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityOOMKiller.class);

  private final SingularityClient singularity;
  private final MesosClient mesos;
  private final SingularityOOMKillerConfiguration oomKillerConfiguration;

  @Inject
  public SingularityOOMKiller(MesosClient mesos, SingularityOOMKillerConfiguration oomKillerConfiguration, SingularityClient singularity) {
    this.mesos = mesos;
    this.oomKillerConfiguration = oomKillerConfiguration;
    this.singularity = singularity;
  }
  
  public void checkForOOMS() {
    List<MesosTaskMonitorObject> taskMonitors = mesos.getSlaveResourceUsage("localhost");
    
    for (MesosTaskMonitorObject taskMonitor : taskMonitors) {
    }
    
  }
  
  
}
