package com.hubspot.singularity.mesos;

import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.SingularityModule;

public class SingularityDriver {
  
  public static final String FRAMEWORK_NAME = "Singularity-0.0.1";

  private final Protos.FrameworkInfo frameworkInfo;
  private final MesosSchedulerDriver driver;

  @Inject
  public SingularityDriver(@Named(SingularityModule.MASTER_PROPERTY) String master, SingularityScheduler scheduler) {
    frameworkInfo = Protos.FrameworkInfo.newBuilder()
        .setCheckpoint(false)
        .setFailoverTimeout(1)
        .setName(FRAMEWORK_NAME)
        .setUser("")  // let mesos assign
        .build();
  
    driver = new MesosSchedulerDriver(scheduler, frameworkInfo, master);
  }

  public void start() {
    System.out.println("starting driver...");

    driver.start();
  }
  
}