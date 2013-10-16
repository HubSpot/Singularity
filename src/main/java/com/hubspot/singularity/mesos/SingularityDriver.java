package com.hubspot.singularity.mesos;

import com.google.common.collect.ImmutableList;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;

import java.util.List;

public class SingularityDriver implements Runnable {
  public static final String FRAMEWORK_NAME = "Singularity-0.0.1";

  private final Protos.FrameworkInfo frameworkInfo;
  private final SingularityScheduler scheduler;
  private final MesosSchedulerDriver driver;
  private final List<Protos.Resource> desiredResources;

  public SingularityDriver(String master, List<Protos.Resource> desiredResources, Protos.CommandInfo commandInfo) {
    frameworkInfo = Protos.FrameworkInfo.newBuilder()
        .setCheckpoint(false)
        .setFailoverTimeout(1)
        .setName(FRAMEWORK_NAME)
        .setUser("")  // let mesos assign
        .build();
    scheduler = new SingularityScheduler(desiredResources, commandInfo);
    driver = new MesosSchedulerDriver(scheduler, frameworkInfo, master);
    this.desiredResources = desiredResources;
  }

  @Override
  public void run() {
    System.out.println("Running driver...");

    System.out.println("Status: " + driver.start().name());
    System.out.println("Requesting resources...");
    driver.requestResources(ImmutableList.of(Protos.Request.newBuilder().addAllResources(desiredResources).build()));
    System.out.println("Joining...");
    driver.join();
  }
}
