package com.hubspot.singularity.mesos;

import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityModule;
import com.hubspot.singularity.config.MesosConfiguration;

public class SingularityDriver {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityDriver.class);
  
  private final Protos.FrameworkInfo frameworkInfo;
  private final SingularityMesosSchedulerDelegator scheduler;
  private final MesosSchedulerDriver driver;

  @Inject
  public SingularityDriver(@Named(SingularityModule.MASTER_PROPERTY) String master, SingularityMesosSchedulerDelegator scheduler, MesosConfiguration configuration) {
    this.frameworkInfo = Protos.FrameworkInfo.newBuilder()
        .setCheckpoint(configuration.getCheckpoint())
        .setFailoverTimeout(configuration.getFrameworkFailoverTimeout())
        .setName(configuration.getFrameworkName())
        .setId(FrameworkID.newBuilder().setValue(configuration.getFrameworkId()))
        .setUser("")  // let mesos assign
        .build();
  
    this.scheduler = scheduler;
    
    this.driver = new MesosSchedulerDriver(scheduler, frameworkInfo, master);
  }
  
  @VisibleForTesting
  public Scheduler getScheduler() {
    return scheduler;
  }
  
  public MasterInfo getMaster() {
    return scheduler.getMaster();
  }
  
  public Optional<Long> getLastOfferTimestamp() {
    return scheduler.getLastOfferTimestamp();
  }
  
  public Protos.Status start() {
    Protos.Status status = driver.start();
  
    LOG.info("Started with status: {}", status);

    return status;
  }
  
  public Protos.Status kill(String taskId) {
    Protos.Status status = driver.killTask(TaskID.newBuilder().setValue(taskId).build());
  
    LOG.info("Killed task {} with driver status: {}", taskId, status);
    
    return status;
  }
  
  public Protos.Status abort() {
    scheduler.notifyStopping();
    
    Protos.Status status = driver.abort();
    
    LOG.info("Aborted with status: {}", status);
        
    return status;
  }
  
}