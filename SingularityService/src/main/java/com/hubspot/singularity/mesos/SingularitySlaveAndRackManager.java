package com.hubspot.singularity.mesos;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Attribute;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.SlaveID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosMasterSlaveObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.AbstractMachineManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
class SingularitySlaveAndRackManager {

  private static final Logger LOG = LoggerFactory.getLogger(SingularitySlaveAndRackManager.class);

  private final String rackIdAttributeKey;
  private final String defaultRackId;

  private final SingularityConfiguration configuration;

  private final SingularityExceptionNotifier exceptionNotifier;
  private final RackManager rackManager;
  private final SlaveManager slaveManager;
  private final TaskManager taskManager;

  @Inject
  SingularitySlaveAndRackManager(SingularityConfiguration configuration, SingularityExceptionNotifier exceptionNotifier, RackManager rackManager, SlaveManager slaveManager, TaskManager taskManager) {
    this.configuration = configuration;

    MesosConfiguration mesosConfiguration = configuration.getMesosConfiguration();

    this.exceptionNotifier = exceptionNotifier;
    this.rackIdAttributeKey = mesosConfiguration.getRackIdAttributeKey();
    this.defaultRackId = mesosConfiguration.getDefaultRackId();

    this.rackManager = rackManager;
    this.slaveManager = slaveManager;
    this.taskManager = taskManager;
  }

  public enum SlaveMatchState {
    OK(true),
    NOT_RACK_OR_SLAVE_PARTICULAR(true),
    RACK_SATURATED(false),
    SLAVE_SATURATED(false),
    SLAVE_DECOMMISSIONING(false),
    RACK_DECOMMISSIONING(false),
    RACK_AFFINITY_NOT_MATCHING(false);

    private final boolean isMatchAllowed;

    private SlaveMatchState(boolean isMatchAllowed) {
      this.isMatchAllowed = isMatchAllowed;
    }

    public boolean isMatchAllowed() {
      return isMatchAllowed;
    }

  }

  private String getHost(String hostname) {
    if (configuration.getCommonHostnameSuffixToOmit().isPresent()) {
      if (hostname.endsWith(configuration.getCommonHostnameSuffixToOmit().get())) {
        hostname = hostname.substring(0, hostname.length() - configuration.getCommonHostnameSuffixToOmit().get().length());
      }
    }
    return getSafeString(hostname);
  }

  public String getSlaveHost(Offer offer) {
    return getHost(offer.getHostname());
  }

  public SlaveMatchState doesOfferMatch(Protos.Offer offer, SingularityTaskRequest taskRequest, SingularitySchedulerStateCache stateCache) {
    final String host = getSlaveHost(offer);
    final String rackId = getRackId(offer);
    final String slaveId = offer.getSlaveId().getValue();

    if (stateCache.getSlave(slaveId).get().getCurrentState().getState().isDecommissioning()) {
      return SlaveMatchState.SLAVE_DECOMMISSIONING;
    }

    if (stateCache.getRack(rackId).get().getCurrentState().getState().isDecommissioning()) {
      return SlaveMatchState.RACK_DECOMMISSIONING;
    }

    if (!taskRequest.getRequest().getRackAffinity().or(Collections.<String> emptyList()).isEmpty()) {
      if (!taskRequest.getRequest().getRackAffinity().get().contains(rackId)) {
        LOG.trace("Task {} requires a rack in {} (current rack {})", taskRequest.getPendingTask().getPendingTaskId(), taskRequest.getRequest().getRackAffinity().get(), rackId);
        return SlaveMatchState.RACK_AFFINITY_NOT_MATCHING;
      }
    }

    final SlavePlacement slavePlacement = taskRequest.getRequest().getSlavePlacement().or(configuration.getDefaultSlavePlacement());

    if (!taskRequest.getRequest().isRackSensitive() && slavePlacement == SlavePlacement.GREEDY) {
      return SlaveMatchState.NOT_RACK_OR_SLAVE_PARTICULAR;
    }

    final int numDesiredInstances = taskRequest.getRequest().getInstancesSafe();
    double numOnRack = 0;
    double numOnSlave = 0;
    double numCleaningOnSlave = 0;

    Collection<SingularityTaskId> cleaningTasks = stateCache.getCleaningTasks();

    for (SingularityTaskId taskId : SingularityTaskId.matchingAndNotIn(stateCache.getActiveTaskIds(), taskRequest.getRequest().getId(), taskRequest.getDeploy().getId(), Collections.<SingularityTaskId>emptyList())) {
      // TODO consider using executorIds
      if (taskId.getHost().equals(host)) {
        if (cleaningTasks.contains(taskId)) {
          numCleaningOnSlave++;
        } else {
          numOnSlave++;
        }
      }
      if (taskId.getRackId().equals(rackId) && !cleaningTasks.contains(taskId)) {
        numOnRack++;
      }
    }

    if (taskRequest.getRequest().isRackSensitive()) {
      final double numPerRack = numDesiredInstances / (double) stateCache.getNumActiveRacks();

      final boolean isRackOk = numOnRack < numPerRack;

      if (!isRackOk) {
        LOG.trace("Rejecting RackSensitive task {} from slave {} ({}) due to numOnRack {} and cleaningOnSlave {}", taskRequest.getRequest().getId(), slaveId, host, numOnRack, numCleaningOnSlave);
        return SlaveMatchState.RACK_SATURATED;
      }
    }

    switch (slavePlacement) {
      case SEPARATE:
        if (numOnSlave > 0 || numCleaningOnSlave > 0) {
          LOG.trace("Rejecting SEPARATE task {} from slave {} ({}) due to numOnSlave {}", taskRequest.getRequest().getId(), slaveId, host, numOnSlave);
          return SlaveMatchState.SLAVE_SATURATED;
        }
        break;
      case OPTIMISTIC:
        final double numPerSlave = numDesiredInstances / (double) stateCache.getNumActiveSlaves();

        final boolean isSlaveOk = numOnSlave < numPerSlave;

        if (!isSlaveOk) {
          LOG.trace("Rejecting OPTIMISTIC task {} from slave {} ({}) due to numOnSlave {}", taskRequest.getRequest().getId(), slaveId, host, numOnSlave);
          return SlaveMatchState.SLAVE_SATURATED;
        }
        break;
      case GREEDY:
    }

    return SlaveMatchState.OK;
  }

  public void slaveLost(SlaveID slaveIdObj) {
    final String slaveId = slaveIdObj.getValue();

    Optional<SingularitySlave> slave = slaveManager.getObject(slaveId);

    if (slave.isPresent()) {
      slaveManager.changeState(slave.get(), MachineState.DEAD, Optional.<String> absent());

      checkRackAfterSlaveLoss(slave.get());
    } else {
      LOG.warn("Lost a slave {}, but didn't know about it", slaveId);
    }
  }

  private void checkRackAfterSlaveLoss(SingularitySlave lostSlave) {
    List<SingularitySlave> slaves = slaveManager.getObjectsFiltered(MachineState.ACTIVE);

    int numInRack = 0;

    for (SingularitySlave slave : slaves) {
      if (slave.getRackId().equals(lostSlave.getRackId())) {
        numInRack++;
      }
    }

    LOG.info("Found {} slaves left in rack {}", numInRack, lostSlave.getRackId());

    if (numInRack == 0) {
      rackManager.changeState(lostSlave.getRackId(), MachineState.DEAD, Optional.<String> absent());
    }
  }

  public void loadSlavesAndRacksFromMaster(MesosMasterStateObject state) {
    Map<String, SingularitySlave> activeSlavesById = slaveManager.getObjectsByIdForState(MachineState.ACTIVE);
    Map<String, SingularityRack> activeRacksById = rackManager.getObjectsByIdForState(MachineState.ACTIVE);

    Map<String, SingularityRack> remainingActiveRacks = Maps.newHashMap(activeRacksById);

    int slaves = 0;
    int racks = 0;

    for (MesosMasterSlaveObject slaveJsonObject : state.getSlaves()) {
      Optional<String> maybeRackId = Optional.fromNullable(slaveJsonObject.getAttributes().get(rackIdAttributeKey));
      String slaveId = slaveJsonObject.getId();
      String rackId = getSafeString(maybeRackId.or(defaultRackId));
      String host = getHost(slaveJsonObject.getHostname());

      if (activeSlavesById.containsKey(slaveId)) {
        activeSlavesById.remove(slaveId);
      } else {
        SingularitySlave newSlave = new SingularitySlave(slaveId, host, rackId);

        if (check(newSlave, slaveManager) == CheckResult.NEW) {
          slaves++;
        }
      }

      if (activeRacksById.containsKey(rackId)) {
        remainingActiveRacks.remove(rackId);
      } else {
        SingularityRack rack = new SingularityRack(rackId);

        if (check(rack, rackManager) == CheckResult.NEW) {
          racks++;
        }
      }
    }

    for (SingularitySlave leftOverSlave : activeSlavesById.values()) {
      slaveManager.changeState(leftOverSlave, MachineState.MISSING_ON_STARTUP, Optional.<String> absent());
    }

    for (SingularityRack leftOverRack : remainingActiveRacks.values()) {
      rackManager.changeState(leftOverRack, MachineState.MISSING_ON_STARTUP, Optional.<String> absent());
    }

    LOG.info("Found {} new racks ({} missing) and {} new slaves ({} missing)", racks, remainingActiveRacks.size(), slaves, activeSlavesById.size());
  }

  public String getRackId(Offer offer) {
    for (Attribute attribute : offer.getAttributesList()) {
      if (attribute.getName().equals(rackIdAttributeKey)) {
        return getSafeString(attribute.getText().getValue());
      }
    }

    return defaultRackId;
  }

  private String getSafeString(String string) {
    return string.replace("-", "_");
  }

  private enum CheckResult {
    NEW, DECOMMISSIONING, ALREADY_ACTIVE;
  }

  private <T extends SingularityMachineAbstraction<T>> CheckResult check(T object, AbstractMachineManager<T> manager) {
    Optional<T> existingObject = manager.getObject(object.getId());

    if (!existingObject.isPresent()) {
      manager.saveObject(object);

      return CheckResult.NEW;
    }

    MachineState currentState = existingObject.get().getCurrentState().getState();

    switch (currentState) {
      case ACTIVE:
        return CheckResult.ALREADY_ACTIVE;
      case DEAD:
      case MISSING_ON_STARTUP:
        manager.changeState(object.getId(), MachineState.ACTIVE, Optional.<String> absent());
        return CheckResult.NEW;
      case DECOMMISSIONED:
      case DECOMMISSIONING:
      case STARTING_DECOMMISSION:
        return CheckResult.DECOMMISSIONING;
    }

    throw new IllegalStateException(String.format("Invalid state %s for %s", currentState, object.getId()));
  }

  public void checkOffer(Offer offer) {
    final String slaveId = offer.getSlaveId().getValue();
    final String rackId = getRackId(offer);
    final String host = getSlaveHost(offer);

    final SingularitySlave slave = new SingularitySlave(slaveId, host, rackId);

    if (check(slave, slaveManager) == CheckResult.NEW) {
      LOG.info("Offer revealed a new slave {}", slave);
    }

    final SingularityRack rack = new SingularityRack(rackId);

    if (check(rack, rackManager) == CheckResult.NEW) {
      LOG.info("Offer revealed a new rack {}", rack);
    }
  }

  public void checkStateAfterFinishedTask(SingularityTaskId taskId, String slaveId, SingularitySchedulerStateCache stateCache) {
    Optional<SingularitySlave> slave = slaveManager.getObject(slaveId);

    if (!slave.isPresent()) {
      final String message = String.format("Couldn't find slave with id %s for task %s", slaveId, taskId);
      LOG.warn(message);
      exceptionNotifier.notify(message, ImmutableMap.of("slaveId", slaveId, "taskId", taskId.toString()));
      return;
    }

    if (slave.get().getCurrentState().getState() == MachineState.DECOMMISSIONING) {
      if (!hasTaskLeftOnSlave(taskId, slaveId, stateCache)) {
        slaveManager.changeState(slave.get(), MachineState.DECOMMISSIONED, slave.get().getCurrentState().getUser());
      }
    }

    Optional<SingularityRack> rack = rackManager.getObject(taskId.getRackId());

    if (!rack.isPresent()) {
      final String message = String.format("Couldn't find rack with id %s for task %s", taskId.getRackId(), taskId);
      LOG.warn(message);
      exceptionNotifier.notify(message, ImmutableMap.of("rackId", taskId.getRackId(), "taskId", taskId.toString()));
      return;
    }

    if (rack.get().getCurrentState().getState() == MachineState.DECOMMISSIONING) {
      if (!hasTaskLeftOnRack(taskId, stateCache)) {
        rackManager.changeState(rack.get(), MachineState.DECOMMISSIONED, rack.get().getCurrentState().getUser());
      }
    }
  }

  private boolean hasTaskLeftOnRack(SingularityTaskId taskId, SingularitySchedulerStateCache stateCache) {
    for (SingularityTaskId activeTaskId : stateCache.getActiveTaskIds()) {
      if (!activeTaskId.equals(taskId) && activeTaskId.getRackId().equals(taskId.getRackId())) {
        return true;
      }
    }

    return false;
  }

  private boolean hasTaskLeftOnSlave(SingularityTaskId taskId, String slaveId, SingularitySchedulerStateCache stateCache) {
    for (SingularityTaskId activeTaskId : stateCache.getActiveTaskIds()) {
      if (!activeTaskId.equals(taskId) && activeTaskId.getHost().equals(taskId.getHost())) {
        Optional<SingularityTask> maybeTask = taskManager.getTask(activeTaskId);
        if (maybeTask.isPresent() && slaveId.equals(maybeTask.get().getOffer().getSlaveId().getValue())) {
          return true;
        }
      };
    }

    return false;
  }

}
