package com.hubspot.singularity.mesos;

import java.io.IOException;

import javax.inject.Singleton;

import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.groupon.mesos.JesosSchedulerDriver;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityDriver {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDriver.class);

  private final Protos.FrameworkInfo frameworkInfo;
  private final SingularityMesosSchedulerDelegator scheduler;
  private final SchedulerDriver driver;

  @Inject
  SingularityDriver(final SingularityMesosSchedulerDelegator scheduler, final SingularityConfiguration singularityConfiguration, final MesosConfiguration configuration) throws IOException {
    FrameworkInfo.Builder frameworkInfoBuilder = Protos.FrameworkInfo.newBuilder()
        .setCheckpoint(configuration.getCheckpoint())
        .setFailoverTimeout(configuration.getFrameworkFailoverTimeout())
        .setName(configuration.getFrameworkName())
        .setId(FrameworkID.newBuilder().setValue(configuration.getFrameworkId()))
        .setUser("");  // let mesos assign

    if (singularityConfiguration.getHostname().isPresent()) {
      frameworkInfoBuilder.setHostname(singularityConfiguration.getHostname().get());
    }

    if (singularityConfiguration.getUiConfiguration().getBaseUrl().isPresent()) {
      frameworkInfoBuilder.setWebuiUrl(singularityConfiguration.getUiConfiguration().getBaseUrl().get());
    }

    this.frameworkInfo = frameworkInfoBuilder.build();

    this.scheduler = scheduler;

    if (configuration.isUseNativeCode()) {
        this.driver = new MesosSchedulerDriver(scheduler, frameworkInfo, configuration.getMaster());
    } else {
        this.driver = new JesosSchedulerDriver(scheduler, frameworkInfo, configuration.getMaster());
    }
  }

  @VisibleForTesting
  public Scheduler getScheduler() {
    return scheduler;
  }

  public Optional<MasterInfo> getMaster() {
    return scheduler.getMaster();
  }

  public Optional<Long> getLastOfferTimestamp() {
    return scheduler.getLastOfferTimestamp();
  }

  public Protos.Status start() {
    LOG.info("Calling driver.start() ...");

    Protos.Status status = driver.start();

    LOG.info("Started with status: {}", status);

    return status;
  }

  public Protos.Status kill(SingularityTaskId taskId) {
    Protos.Status status = driver.killTask(TaskID.newBuilder().setValue(taskId.toString()).build());

    LOG.info("Killed task {} with driver status: {}", taskId, status);

    return status;
  }

  public Protos.Status abort() {
    LOG.info("Notifying scheduler about impending driver abort");

    scheduler.notifyStopping();

    LOG.info("Calling driver.abort() ...");

    Protos.Status status = driver.abort();

    LOG.info("Aborted with status: {}", status);

    return status;
  }

}
