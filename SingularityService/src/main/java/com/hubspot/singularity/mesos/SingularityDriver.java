package com.hubspot.singularity.mesos;

import java.io.IOException;

import javax.inject.Singleton;

import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Credential;
import org.apache.mesos.Protos.ExecutorID;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.UIConfiguration;
import com.hubspot.singularity.resources.UiResource;

@Singleton
public class SingularityDriver {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDriver.class);

  private final Protos.FrameworkInfo frameworkInfo;
  private final SingularityMesosSchedulerDelegator scheduler;
  private final SchedulerDriver driver;

  @Inject
  SingularityDriver(final SingularityMesosSchedulerDelegator scheduler, final SingularityConfiguration singularityConfiguration, final MesosConfiguration configuration,
                    @Named(SingularityMainModule.SINGULARITY_URI_BASE) final String singularityUriBase) throws IOException {
    final FrameworkInfo.Builder frameworkInfoBuilder = Protos.FrameworkInfo.newBuilder()
        .setCheckpoint(configuration.getCheckpoint())
        .setFailoverTimeout(configuration.getFrameworkFailoverTimeout())
        .setName(configuration.getFrameworkName())
        .setId(FrameworkID.newBuilder().setValue(configuration.getFrameworkId()))
        .setUser("");  // let mesos assign

    if (singularityConfiguration.getHostname().isPresent()) {
      frameworkInfoBuilder.setHostname(singularityConfiguration.getHostname().get());
    }

    // only set the web UI URL if it's fully qualified
    if (singularityUriBase.startsWith("http://") || singularityUriBase.startsWith("https://")) {
      if (singularityConfiguration.getUiConfiguration().getRootUrlMode() == UIConfiguration.RootUrlMode.INDEX_CATCHALL) {
        frameworkInfoBuilder.setWebuiUrl(singularityUriBase);
      } else {
        frameworkInfoBuilder.setWebuiUrl(singularityUriBase + UiResource.UI_RESOURCE_LOCATION);
      }
    }

    if (configuration.getFrameworkRole().isPresent()) {
      frameworkInfoBuilder.setRole(configuration.getFrameworkRole().get());
    }

    this.frameworkInfo = frameworkInfoBuilder.build();

    this.scheduler = scheduler;

    if (configuration.getCredentialPrincipal().isPresent() && configuration.getCredentialSecret().isPresent()) {
      Credential credential = Credential.newBuilder()
        .setPrincipal(configuration.getCredentialPrincipal().get())
        .setSecret(configuration.getCredentialSecret().get())
        .build();
      this.driver = new MesosSchedulerDriver(scheduler, frameworkInfo, configuration.getMaster(), false, credential);
    } else {
      this.driver = new MesosSchedulerDriver(scheduler, frameworkInfo, configuration.getMaster(), false);
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

  public Protos.Status sendFrameworkMessage(SingularityTaskId taskId, ExecutorID executorID, SlaveID slaveID, byte [] bytes) {
    Protos.Status status = driver.sendFrameworkMessage(executorID, slaveID, bytes);
    LOG.info("Sent framework message for task {} with driver status: {}", taskId, status);
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
