package com.hubspot.singularity.mesos;

import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.FrameworkID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityModule;
import com.hubspot.singularity.config.MesosConfiguration;

public class SingularityDriver {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);
  
  private final Protos.FrameworkInfo frameworkInfo;
  private final MesosSchedulerDriver driver;

  @Inject
  public SingularityDriver(@Named(SingularityModule.MASTER_PROPERTY) String master, SingularityMesosScheduler scheduler, MesosConfiguration configuration) {
    frameworkInfo = Protos.FrameworkInfo.newBuilder()
        .setCheckpoint(false)
        .setFailoverTimeout(configuration.getFrameworkFailoverTimeout())
        .setName(configuration.getFrameworkName())
        .setId(FrameworkID.newBuilder().setValue(configuration.getFrameworkId()))
        .setUser("")  // let mesos assign
        .build();
  
    driver = new MesosSchedulerDriver(scheduler, frameworkInfo, master);
  }

  public void start() {
    LOG.info("Mesos driver is starting with framework info: " + frameworkInfo);

    driver.start();
  }
  
  public void stop() {
    LOG.info("Mesos driver stopping..");
    
    driver.stop();
  }
  
}