package com.hubspot.singularity.mesos;

import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityModule;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityDriver {

  private final Protos.FrameworkInfo frameworkInfo;
  private final MesosSchedulerDriver driver;

  @Inject
  public SingularityDriver(@Named(SingularityModule.MASTER_PROPERTY) String master, SingularityScheduler scheduler, SingularityConfiguration configuration) {
    frameworkInfo = Protos.FrameworkInfo.newBuilder()
        .setCheckpoint(false)
        .setFailoverTimeout(configuration.getMesosConfiguration().getFrameworkFailoverTimeout())
        .setName(configuration.getMesosConfiguration().getFrameworkName())
        .setUser("")  // let mesos assign
        .build();
  
    driver = new MesosSchedulerDriver(scheduler, frameworkInfo, master);
  }

  public void start() {
    System.out.println("starting driver...");

    driver.start();
  }
  
  public void stop() {
    driver.stop();
  }
  
}