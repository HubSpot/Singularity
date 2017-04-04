package com.hubspot.singularity.scheduler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskShellCommandRequestId;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.hubspot.singularity.expiring.SingularityExpiringParent;
import com.hubspot.singularity.expiring.SingularityExpiringPause;
import com.hubspot.singularity.expiring.SingularityExpiringRequestActionParent;
import com.hubspot.singularity.expiring.SingularityExpiringScale;
import com.hubspot.singularity.expiring.SingularityExpiringSkipHealthchecks;
import com.hubspot.singularity.helpers.RequestHelper;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;
import com.hubspot.singularity.smtp.SingularityMailer;

@Singleton
public class SingularityExpiringUserActionPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityExpiringUserActionPoller.class);

  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final TaskManager taskManager;
  private final SingularityMailer mailer;
  private final RequestHelper requestHelper;
  private final SlaveManager slaveManager;
  private final RackManager rackManager;
  private final List<SingularityExpiringUserActionHandler<?, ?>> handlers;
  private final SingularityConfiguration configuration;
  private final DisasterManager disasterManager;

  @Inject
  SingularityExpiringUserActionPoller(SingularityConfiguration configuration, RequestManager requestManager, DeployManager deployManager, TaskManager taskManager, SlaveManager slaveManager, RackManager rackManager,
      SingularitySchedulerLock lock, RequestHelper requestHelper, SingularityMailer mailer, DisasterManager disasterManager) {
    super(configuration.getCheckExpiringUserActionEveryMillis(), TimeUnit.MILLISECONDS, lock);

    this.deployManager = deployManager;
    this.requestManager = requestManager;
    this.requestHelper = requestHelper;
    this.mailer = mailer;
    this.taskManager = taskManager;
    this.slaveManager = slaveManager;
    this.rackManager = rackManager;
    this.configuration = configuration;
    this.disasterManager = disasterManager;

    List<SingularityExpiringUserActionHandler<?, ?>> tempHandlers = Lists.newArrayList();
    tempHandlers.add(new SingularityExpiringBounceHandler());
    tempHandlers.add(new SingularityExpiringPauseHandler());
    tempHandlers.add(new SingularityExpiringScaleHandler());
    tempHandlers.add(new SingularityExpiringSkipHealthchecksHandler());
    tempHandlers.add(new SingularityExpiringSlaveStateHandler());
    tempHandlers.add(new SingularityExpiringRackStateHandler());

    this.handlers = ImmutableList.copyOf(tempHandlers);
  }

  @Override
  public void runActionOnPoll() {
    if (disasterManager.isDisabled(SingularityAction.RUN_EXPIRING_ACTION_POLLER)) {
      LOG.warn("Expiring user action poller is disabled");
      return;
    }
    for (SingularityExpiringUserActionHandler<?, ?> handler : handlers) {
      handler.checkExpiringObjects();
    }
  }

  private abstract class SingularityExpiringUserActionHandler<T extends SingularityExpiringParent<?>, Q> {
    private final Class<T> clazz;

    private SingularityExpiringUserActionHandler(Class<T> clazz) {
      this.clazz = clazz;
    }

    protected Class<T> getClazz() {
      return clazz;
    }

    protected boolean isExpiringDue(T expiringObject) {
      final long now = System.currentTimeMillis();
      final long duration = now - expiringObject.getStartMillis();

      return duration > getDurationMillis(expiringObject);
    }

    protected String getMessage(T expiringObject) {
      String msg = String.format("%s expired after %s", getActionName(),
          JavaUtils.durationFromMillis(getDurationMillis(expiringObject)));
      if (expiringObject.getExpiringAPIRequestObject().getMessage().isPresent() && expiringObject.getExpiringAPIRequestObject().getMessage().get().length() > 0) {
        msg = String.format("%s (%s)", msg, expiringObject.getExpiringAPIRequestObject().getMessage().get());
      }
      return msg;
    }

    protected long getDurationMillis(T expiringObject) {
      return expiringObject.getExpiringAPIRequestObject().getDurationMillis().get();
    }

    protected abstract void checkExpiringObjects();
    protected abstract String getActionName();
    protected abstract void handleExpiringObject(T expiringObject, Q targetObject, String message);

  }

  // Expiring request-related actions
  private abstract class SingularityExpiringRequestActionHandler<T extends SingularityExpiringRequestActionParent<?>> extends SingularityExpiringUserActionHandler<T, SingularityRequestWithState> {

    private SingularityExpiringRequestActionHandler(Class<T> clazz) {
      super(clazz);
    }

    @Override
    protected void checkExpiringObjects() {
      for (T expiringObject : requestManager.getExpiringObjects(getClazz())) {
        if (isExpiringDue(expiringObject)) {

          Optional<SingularityRequestWithState> requestWithState = requestManager.getRequest(expiringObject.getRequestId());

          if (!requestWithState.isPresent()) {
            LOG.warn("Request {} not present, discarding {}", expiringObject.getRequestId(), expiringObject);
          } else {
            handleExpiringObject(expiringObject, requestWithState.get(), getMessage(expiringObject));
          }

          requestManager.deleteExpiringObject(getClazz(), expiringObject.getRequestId());
        }
      }
    }
  }


  private class SingularityExpiringBounceHandler extends SingularityExpiringRequestActionHandler<SingularityExpiringBounce> {

    public SingularityExpiringBounceHandler() {
      super(SingularityExpiringBounce.class);
    }

    @Override
    protected String getActionName() {
      return "Bounce";
    }

    @Override
    protected long getDurationMillis(SingularityExpiringBounce expiringBounce) {
      return expiringBounce.getExpiringAPIRequestObject().getDurationMillis().or(TimeUnit.MINUTES.toMillis(configuration.getDefaultBounceExpirationMinutes()));
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringBounce expiringObject, SingularityRequestWithState requestWithState, String message) {
      for (SingularityTaskCleanup taskCleanup : taskManager.getCleanupTasks()) {
        if (taskCleanup.getTaskId().getRequestId().equals(expiringObject.getRequestId())
            && taskCleanup.getActionId().isPresent() && expiringObject.getActionId().equals(taskCleanup.getActionId().get())) {
          LOG.info("Discarding cleanup for {} ({}) because of {}", taskCleanup.getTaskId(), taskCleanup, expiringObject);
          taskManager.deleteCleanupTask(taskCleanup.getTaskId().getId());
          if (!taskManager.getTaskCleanup(taskCleanup.getTaskId().getId()).isPresent()) {
            LOG.info("No other task cleanups found, removing task cleanup update for {}", taskCleanup.getTaskId());
            List<SingularityTaskHistoryUpdate> historyUpdates = taskManager.getTaskHistoryUpdates(taskCleanup.getTaskId());
            Collections.sort(historyUpdates);
            if (Iterables.getLast(historyUpdates).getTaskState() == ExtendedTaskState.TASK_CLEANING) {
              Optional<SingularityTaskHistoryUpdate> maybePreviousHistoryUpdate = historyUpdates.size() > 1 ? Optional.of(historyUpdates.get(historyUpdates.size() - 2)) : Optional.<SingularityTaskHistoryUpdate>absent();
              taskManager.deleteTaskHistoryUpdate(taskCleanup.getTaskId(), ExtendedTaskState.TASK_CLEANING, maybePreviousHistoryUpdate);
            }
          }
        }
      }

      Optional<SingularityPendingRequest> pendingRequest = requestManager.getPendingRequest(expiringObject.getRequestId(), expiringObject.getDeployId());

      if (pendingRequest.isPresent() && pendingRequest.get().getActionId().isPresent() && pendingRequest.get().getActionId().get().equals(expiringObject.getActionId())) {
        LOG.info("Discarding pending request for {} ({}) because of {}", expiringObject.getRequestId(), pendingRequest.get(), expiringObject);

        requestManager.deletePendingRequest(pendingRequest.get());
      }

      requestManager.addToPendingQueue(new SingularityPendingRequest(expiringObject.getRequestId(), expiringObject.getDeployId(), System.currentTimeMillis(), expiringObject.getUser(),
          PendingType.CANCEL_BOUNCE, Optional.<List<String>> absent(), Optional.<String> absent(), Optional.<Boolean> absent(), Optional.of(message), Optional.of(expiringObject.getActionId())));
    }

  }

  private class SingularityExpiringPauseHandler extends SingularityExpiringRequestActionHandler<SingularityExpiringPause> {

    public SingularityExpiringPauseHandler() {
      super(SingularityExpiringPause.class);
    }

    @Override
    protected String getActionName() {
      return "Pause";
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringPause expiringObject, SingularityRequestWithState requestWithState, String message) {
      if (requestWithState.getState() != RequestState.PAUSED) {
        LOG.warn("Discarding {} because request {} is in state {}", expiringObject, requestWithState.getRequest().getId(), requestWithState.getState());
        return;
      }

      LOG.info("Unpausing request {} because of {}", requestWithState.getRequest().getId(), expiringObject);

      requestHelper.unpause(requestWithState.getRequest(), expiringObject.getUser(), Optional.of(message), Optional.<Boolean> absent());
    }

  }

  private class SingularityExpiringScaleHandler extends SingularityExpiringRequestActionHandler<SingularityExpiringScale> {

    public SingularityExpiringScaleHandler() {
      super(SingularityExpiringScale.class);
    }

    @Override
    protected String getActionName() {
      return "Scale";
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringScale expiringObject, SingularityRequestWithState requestWithState, String message) {
      final SingularityRequest oldRequest = requestWithState.getRequest();
      final SingularityRequest newRequest = oldRequest.toBuilder().setInstances(expiringObject.getRevertToInstances()).build();

      try {
        Optional<SingularityBounceRequest> maybeBounceRequest = Optional.absent();

        if (expiringObject.getBounce().or(false) || newRequest.getBounceAfterScale().or(false)) {
          LOG.info("Attempting to bounce request {} after expiring scale", newRequest.getId());
          Optional<String> maybeActiveDeployId = deployManager.getInUseDeployId(newRequest.getId());
          if (maybeActiveDeployId.isPresent()) {
            maybeBounceRequest = Optional.of(SingularityBounceRequest.defaultRequest());
          } else {
            LOG.debug("No active deploy id present for request {}, not bouncing after expiring scale", newRequest.getId());
          }
        }

        requestHelper.updateRequest(newRequest, Optional.of(oldRequest), requestWithState.getState(), Optional.of(RequestHistoryType.SCALE_REVERTED), expiringObject.getUser(),
            Optional.<Boolean> absent(), Optional.of(message), maybeBounceRequest);

        mailer.sendRequestScaledMail(newRequest, Optional.<SingularityScaleRequest> absent(), oldRequest.getInstances(), expiringObject.getUser());
      } catch (WebApplicationException wae) {
        LOG.error("While trying to apply {} for {}", expiringObject, expiringObject.getRequestId(), wae);
      }
    }

  }

  private class SingularityExpiringSkipHealthchecksHandler extends SingularityExpiringRequestActionHandler<SingularityExpiringSkipHealthchecks> {

    public SingularityExpiringSkipHealthchecksHandler() {
      super(SingularityExpiringSkipHealthchecks.class);
    }

    @Override
    protected String getActionName() {
      return "Skip healthchecks";
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringSkipHealthchecks expiringObject, SingularityRequestWithState requestWithState, String message) {
      final SingularityRequest oldRequest = requestWithState.getRequest();
      final SingularityRequest newRequest = oldRequest.toBuilder().setSkipHealthchecks(expiringObject.getRevertToSkipHealthchecks()).build();

      try {
        requestHelper.updateRequest(newRequest, Optional.of(oldRequest), requestWithState.getState(), Optional.<RequestHistoryType> absent(), expiringObject.getUser(),
            Optional.<Boolean> absent(), Optional.of(message), Optional.<SingularityBounceRequest>absent());
      } catch (WebApplicationException wae) {
        LOG.error("While trying to apply {} for {}", expiringObject, expiringObject.getRequestId(), wae);
      }
    }
  }

  // Expiring Machine States
  private abstract class SingularityExpiringMachineStateHandler extends SingularityExpiringUserActionHandler<SingularityExpiringMachineState, SingularityMachineAbstraction> {

    public SingularityExpiringMachineStateHandler() {
      super(SingularityExpiringMachineState.class);
    }

    @Override
    protected String getActionName() {
      return "Change machine state";
    }
  }

  private class SingularityExpiringSlaveStateHandler extends SingularityExpiringMachineStateHandler {
    @Override
    protected void handleExpiringObject(SingularityExpiringMachineState expiringObject, SingularityMachineAbstraction machine, String message) {
      SingularitySlave slave = (SingularitySlave) machine;
      slaveManager.changeState(slave, expiringObject.getRevertToState(), Optional.of("Updated due to expiring action"), expiringObject.getUser());
      if (expiringObject.isKillTasksOnDecommissionTimeout() && expiringObject.getRevertToState() == MachineState.DECOMMISSIONED) {
        List<SingularityTaskId> activeTasksIdsOnSlave = taskManager.getActiveTaskIds();
        String sanitizedHost = JavaUtils.getReplaceHyphensWithUnderscores(slave.getHost());
        long now = System.currentTimeMillis();
        for (SingularityTaskId taskId : activeTasksIdsOnSlave) {
          if (taskId.getSanitizedHost().equals(sanitizedHost)) {
            taskManager.saveTaskCleanup(new SingularityTaskCleanup(
              expiringObject.getUser(),
              TaskCleanupType.DECOMMISSION_TIMEOUT,
              now, taskId,
              Optional.of(String.format("Slave decommission (started by: %s) timed out after %sms", expiringObject.getUser(), now - expiringObject.getStartMillis())),
              Optional.<String> absent(),
              Optional.<SingularityTaskShellCommandRequestId> absent()));
          }
        }
      }
    }

    @Override
    protected void checkExpiringObjects() {
      for (SingularityExpiringMachineState expiringObject : slaveManager.getExpiringObjects()) {
        if (isExpiringDue(expiringObject)) {
          Optional<SingularitySlave> slave = slaveManager.getObject(expiringObject.getMachineId());

          if (!slave.isPresent()) {
            LOG.warn("Slave {} not present, discarding {}", expiringObject.getMachineId(), expiringObject);
          } else {
            try {
              handleExpiringObject(expiringObject, slave.get(), getMessage(expiringObject));
            } catch (Exception e) {
              LOG.error("Could not return slave {} to state {}", slave.get().getHost(), expiringObject.getRevertToState());
            }
          }

          slaveManager.deleteExpiringObject(expiringObject.getMachineId());
        }
      }
    }
  }

  private class SingularityExpiringRackStateHandler extends SingularityExpiringMachineStateHandler {
    @Override
    protected void handleExpiringObject(SingularityExpiringMachineState expiringObject, SingularityMachineAbstraction machine, String message) {
      rackManager.changeState((SingularityRack) machine, expiringObject.getRevertToState(), Optional.of("Updated due to expiring action"), expiringObject.getUser());
    }

    @Override
    protected void checkExpiringObjects() {
      for (SingularityExpiringMachineState expiringObject : rackManager.getExpiringObjects()) {
        if (isExpiringDue(expiringObject)) {
          Optional<SingularityRack> rack = rackManager.getObject(expiringObject.getMachineId());

          if (!rack.isPresent()) {
            LOG.warn("Rack {} not present, discarding {}", expiringObject.getMachineId(), expiringObject);
          } else {
            try {
              handleExpiringObject(expiringObject, rack.get(), getMessage(expiringObject));
            } catch (Exception e) {
              LOG.error("Could not return rack {} to state {}", rack.get().getName(), expiringObject.getRevertToState());
            }
          }

          rackManager.deleteExpiringObject(expiringObject.getMachineId());
        }
      }
    }
  }
}
