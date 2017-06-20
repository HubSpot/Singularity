package com.hubspot.singularity.mesos;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.Credential;
import org.apache.mesos.v1.Protos.ExecutorID;
import org.apache.mesos.v1.Protos.FrameworkID;
import org.apache.mesos.v1.Protos.FrameworkInfo;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.Offer.Operation;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.Status;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskInfo;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.scheduler.Protos.Call;
import org.apache.mesos.v1.scheduler.Protos.Call.Decline;
import org.apache.mesos.v1.scheduler.Protos.Call.Reconcile;
import org.apache.mesos.v1.scheduler.Protos.Call.Reconcile.Task;
import org.apache.mesos.v1.scheduler.Protos.Call.Type;
import org.apache.mesos.v1.scheduler.V1Mesos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.UIConfiguration;
import com.hubspot.singularity.resources.UiResource;

@Singleton
public class SingularityDriver {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDriver.class);

  private final SingularityConfiguration singularityConfiguration;
  private final MesosConfiguration mesosConfiguration;
  private final String singularityUriBase;

  private Status currentStatus;
  private V1Mesos driver;
  private FrameworkInfo frameworkInfo;

  @Inject
  SingularityDriver(SingularityConfiguration singularityConfiguration,
                    @Named(SingularityMainModule.SINGULARITY_URI_BASE) final String singularityUriBase) throws IOException {
    this.singularityConfiguration = singularityConfiguration;
    this.mesosConfiguration = singularityConfiguration.getMesosConfiguration();
    this.currentStatus = Status.DRIVER_NOT_STARTED;
    this.singularityUriBase = singularityUriBase;
  }

  public synchronized void start(SingularityMesosScheduler scheduler) {
    final FrameworkInfo.Builder frameworkInfoBuilder = FrameworkInfo.newBuilder()
        .setCheckpoint(mesosConfiguration.getCheckpoint())
        .setFailoverTimeout(mesosConfiguration.getFrameworkFailoverTimeout())
        .setName(mesosConfiguration.getFrameworkName())
        .setId(FrameworkID.newBuilder().setValue(mesosConfiguration.getFrameworkId()))
        .setUser("");

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

    if (mesosConfiguration.getFrameworkRole().isPresent()) {
      frameworkInfoBuilder.setRole(mesosConfiguration.getFrameworkRole().get());
    }

    this.frameworkInfo = frameworkInfoBuilder.build();

    if (mesosConfiguration.getCredentialPrincipal().isPresent() && mesosConfiguration.getCredentialSecret().isPresent()) {
      Credential credential = Credential.newBuilder()
          .setPrincipal(mesosConfiguration.getCredentialPrincipal().get())
          .setSecret(mesosConfiguration.getCredentialSecret().get())
          .build();
      this.driver = new V1Mesos(scheduler, mesosConfiguration.getMaster(), credential);
    } else {
      this.driver = new V1Mesos(scheduler, mesosConfiguration.getMaster());
    }
    this.currentStatus = Status.DRIVER_RUNNING;
  }

  public synchronized void stop() {
    this.driver = null;
    this.frameworkInfo = null;
    this.currentStatus = Status.DRIVER_STOPPED;
  }

  private void checkDriver() {
    Preconditions.checkNotNull(driver, "Tried to send a call but driver was null");
    Preconditions.checkNotNull(frameworkInfo, "Tried to send a call but frameworkInfo was null");
  }

  public boolean isActive() {
    return currentStatus == Status.DRIVER_RUNNING;
  }

  public void subscribe() {
    checkDriver();
    LOG.info("Sending subscribe call");
    Call.Builder callBuilder = Call.newBuilder()
        .setType(Call.Type.SUBSCRIBE)
        .setSubscribe(Call.Subscribe.newBuilder()
            .setFrameworkInfo(frameworkInfo)
            .build());

    driver.send(callBuilder.build());
  }

  public void kill(SingularityTaskId taskId) {
    Call killCall = Call.newBuilder()
        .setType(Type.KILL)
        .setFrameworkId(frameworkInfo.getId())
        .setKill(Call.Kill.newBuilder()
          .setTaskId(TaskID.newBuilder().setValue(taskId.toString()).build())
        ).build();
    driver.send(killCall);
    LOG.info("Killed task {}", taskId);
  }

  public void sendFrameworkMessage(SingularityTaskId taskId, ExecutorID executorID, AgentID agentID, byte [] bytes) {
    Call frameworkMessageCall = Call.newBuilder()
        .setType(Type.MESSAGE)
        .setFrameworkId(frameworkInfo.getId())
        .setMessage(Call.Message.newBuilder()
            .setAgentId(agentID)
            .setExecutorId(executorID)
            .setData(ByteString.copyFrom(bytes)))
        .build();
    driver.send(frameworkMessageCall);
    LOG.info("Sent framework message for task {}", taskId);
  }

  public Status getCurrentStatus() {
    return currentStatus;
  }

  public boolean canKillTask() {
    return currentStatus == Status.DRIVER_RUNNING;
  }

  public void launchTasks(Offer offer, List<TaskInfo> toLaunch) {
    Offer.Operation.Launch.Builder launchBuilder = Offer.Operation.Launch.newBuilder()
        .addAllTaskInfos(toLaunch);

    driver.send(Call.newBuilder()
        .setType(Type.ACCEPT)
        .setFrameworkId(frameworkInfo.getId())
        .setAccept(Call.Accept.newBuilder()
            .addOfferIds(offer.getId())
            .addOperations(Offer.Operation.newBuilder()
                .setType(Operation.Type.LAUNCH)
                .setLaunch(launchBuilder)
                .build())
            .build())
        .build());

    LOG.info("{} tasks ({}) launched", toLaunch.size(), toLaunch.stream().map((TaskInfo::getTaskId)));
  }

  public void declineOffer(OfferID offerID) {
    driver.send(Call.newBuilder()
        .setType(Type.DECLINE)
        .setFrameworkId(frameworkInfo.getId())
        .setDecline(Decline.newBuilder()
            .addOfferIds(offerID)
            .build())
        .build());
  }

  public void reconcileTasks(List<TaskStatus> taskStatuses) {
    driver.send(Call.newBuilder()
        .setType(Type.RECONCILE)
        .setReconcile(Reconcile.newBuilder()
            .addAllTasks(taskStatuses.stream().map((t) -> Task.newBuilder().setTaskId(t.getTaskId()).setAgentId(t.getAgentId()).build()).collect(Collectors.toList()))
            .build())
        .build());
  }
}
