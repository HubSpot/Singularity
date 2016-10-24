package com.hubspot.singularity.mesos;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.SlaveID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.json.MesosMasterSlaveObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosResourcesObject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlaveMatchState;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.AbstractMachineManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularitySlaveAndRackManager {

  private static final Logger LOG = LoggerFactory.getLogger(SingularitySlaveAndRackManager.class);

  private final SingularityConfiguration configuration;

  private final SingularityExceptionNotifier exceptionNotifier;
  private final RackManager rackManager;
  private final SlaveManager slaveManager;
  private final TaskManager taskManager;
  private final SingularitySlaveAndRackHelper slaveAndRackHelper;
  private final AtomicInteger activeSlavesLost;

  @Inject
  SingularitySlaveAndRackManager(SingularitySlaveAndRackHelper slaveAndRackHelper, SingularityConfiguration configuration, SingularityExceptionNotifier exceptionNotifier,
                                 RackManager rackManager, SlaveManager slaveManager, TaskManager taskManager, @Named(SingularityMesosModule.ACTIVE_SLAVES_LOST_COUNTER) AtomicInteger activeSlavesLost) {
    this.configuration = configuration;

    this.exceptionNotifier = exceptionNotifier;
    this.slaveAndRackHelper = slaveAndRackHelper;

    this.rackManager = rackManager;
    this.slaveManager = slaveManager;
    this.taskManager = taskManager;
    this.activeSlavesLost = activeSlavesLost;
  }

  public SlaveMatchState doesOfferMatch(Protos.Offer offer, SingularityTaskRequest taskRequest, SingularitySchedulerStateCache stateCache) {
    final String host = offer.getHostname();
    final String rackId = slaveAndRackHelper.getRackIdOrDefault(offer);
    final String slaveId = offer.getSlaveId().getValue();

    final MachineState currentSlaveState = stateCache.getSlave(slaveId).get().getCurrentState().getState();

    if (currentSlaveState == MachineState.FROZEN) {
      return SlaveMatchState.SLAVE_FROZEN;
    }

    if (currentSlaveState.isDecommissioning()) {
      return SlaveMatchState.SLAVE_DECOMMISSIONING;
    }

    final MachineState currentRackState = stateCache.getRack(rackId).get().getCurrentState().getState();

    if (currentRackState == MachineState.FROZEN) {
      return SlaveMatchState.RACK_FROZEN;
    }

    if (currentRackState.isDecommissioning()) {
      return SlaveMatchState.RACK_DECOMMISSIONING;
    }

    if (!taskRequest.getRequest().getRackAffinity().or(Collections.<String> emptyList()).isEmpty()) {
      if (!taskRequest.getRequest().getRackAffinity().get().contains(rackId)) {
        LOG.trace("Task {} requires a rack in {} (current rack {})", taskRequest.getPendingTask().getPendingTaskId(), taskRequest.getRequest().getRackAffinity().get(), rackId);
        return SlaveMatchState.RACK_AFFINITY_NOT_MATCHING;
      }
    }

    Map<String, String> reservedSlaveAttributes = slaveAndRackHelper.reservedSlaveAttributes(offer);
    if (!reservedSlaveAttributes.isEmpty()) {
      if (taskRequest.getRequest().getRequiredSlaveAttributes().isPresent() || taskRequest.getRequest().getAllowedSlaveAttributes().isPresent()) {
        Map<String, String> mergedAttributes = taskRequest.getRequest().getRequiredSlaveAttributes().or(new HashMap<String, String>());
        mergedAttributes.putAll(taskRequest.getRequest().getAllowedSlaveAttributes().or(new HashMap<String, String>()));
        if (!slaveAndRackHelper.hasRequiredAttributes(mergedAttributes, reservedSlaveAttributes)) {
          LOG.trace("Slaves with attributes {} are reserved for matching tasks. Task with attributes {} does not match", reservedSlaveAttributes, taskRequest.getRequest().getRequiredSlaveAttributes().or(Collections.<String, String>emptyMap()));
          return SlaveMatchState.SLAVE_ATTRIBUTES_DO_NOT_MATCH;
        }
      } else {
        LOG.trace("Slaves with attributes {} are reserved for matching tasks. No attributes specified for task {}", reservedSlaveAttributes, taskRequest.getPendingTask().getPendingTaskId().getId());
        return SlaveMatchState.SLAVE_ATTRIBUTES_DO_NOT_MATCH;
      }
    }

    if (taskRequest.getRequest().getRequiredSlaveAttributes().isPresent()
      && !slaveAndRackHelper.hasRequiredAttributes(slaveAndRackHelper.getTextAttributes(offer), taskRequest.getRequest().getRequiredSlaveAttributes().get())) {
      LOG.trace("Task requires slave with attributes {}, (slave attributes are {})", taskRequest.getRequest().getRequiredSlaveAttributes().get(), slaveAndRackHelper.getTextAttributes(offer));
      return SlaveMatchState.SLAVE_ATTRIBUTES_DO_NOT_MATCH;
    }

    final SlavePlacement slavePlacement = taskRequest.getRequest().getSlavePlacement().or(configuration.getDefaultSlavePlacement());

    if (!taskRequest.getRequest().isRackSensitive() && slavePlacement == SlavePlacement.GREEDY) {
      return SlaveMatchState.NOT_RACK_OR_SLAVE_PARTICULAR;
    }

    final int numDesiredInstances = taskRequest.getRequest().getInstancesSafe();
    Multiset<String> countPerRack = HashMultiset.create(stateCache.getNumActiveRacks());
    double numOnSlave = 0;
    double numCleaningOnSlave = 0;
    double numOtherDeploysOnSlave = 0;

    final String sanitizedHost = JavaUtils.getReplaceHyphensWithUnderscores(host);
    final String sanitizedRackId = JavaUtils.getReplaceHyphensWithUnderscores(rackId);
    Collection<SingularityTaskId> cleaningTasks = stateCache.getCleaningTasks();

    for (SingularityTaskId taskId : SingularityTaskId.matchingAndNotIn(stateCache.getActiveTaskIds(), taskRequest.getRequest().getId(), Collections.<SingularityTaskId>emptyList())) {
      // TODO consider using executorIds
      if (taskId.getSanitizedHost().equals(sanitizedHost)) {
        if (taskRequest.getDeploy().getId().equals(taskId.getDeployId())) {
          if (cleaningTasks.contains(taskId)) {
            numCleaningOnSlave++;
          } else {
            numOnSlave++;
          }
        } else {
          numOtherDeploysOnSlave++;
        }
      }
      if (!cleaningTasks.contains(taskId) && taskRequest.getDeploy().getId().equals(taskId.getDeployId())) {
        countPerRack.add(taskId.getSanitizedRackId());
      }
    }

    if (taskRequest.getRequest().isRackSensitive()) {
      final boolean isRackOk = isRackOk(countPerRack, sanitizedRackId, numDesiredInstances, taskRequest.getRequest().getId(), slaveId, host, numCleaningOnSlave, stateCache);

      if (!isRackOk) {
        return SlaveMatchState.RACK_SATURATED;
      }
    }

    switch (slavePlacement) {
      case SEPARATE:
      case SEPARATE_BY_DEPLOY:
        if (numOnSlave > 0 || numCleaningOnSlave > 0) {
          LOG.trace("Rejecting SEPARATE task {} from slave {} ({}) due to numOnSlave {} numCleaningOnSlave {}", taskRequest.getRequest().getId(), slaveId, host, numOnSlave, numCleaningOnSlave);
          return SlaveMatchState.SLAVE_SATURATED;
        }
        break;
      case SEPARATE_BY_REQUEST:
        if (numOnSlave > 0 || numCleaningOnSlave > 0 || numOtherDeploysOnSlave > 0) {
          LOG.trace("Rejecting SEPARATE task {} from slave {} ({}) due to numOnSlave {} numCleaningOnSlave {} numOtherDeploysOnSlave {}", taskRequest.getRequest().getId(), slaveId, host, numOnSlave, numCleaningOnSlave, numOtherDeploysOnSlave);
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

  private boolean isRackOk(Multiset<String> countPerRack, String sanitizedRackId, int numDesiredInstances, String requestId, String slaveId, String host, double numCleaningOnSlave, SingularitySchedulerStateCache stateCache) {
    int racksAccountedFor = countPerRack.elementSet().size();
    double numPerRack = numDesiredInstances / (double) stateCache.getNumActiveRacks();
    if (racksAccountedFor < stateCache.getNumActiveRacks()) {
      if (countPerRack.count(sanitizedRackId) < Math.max(numPerRack, 1)) {
        return true;
      }
    } else {
      Integer rackMin = null;
      for (String rackId : countPerRack.elementSet()) {
        if (rackMin == null || countPerRack.count(rackId) < rackMin) {
          rackMin = countPerRack.count(rackId);
        }
      }
      if (rackMin == null || rackMin < (int) numPerRack) {
        if (countPerRack.count(sanitizedRackId) < (int) numPerRack) {
          return true;
        }
      } else if (countPerRack.count(sanitizedRackId) < numPerRack) {
        return true;
      }
    }

    LOG.trace("Rejecting RackSensitive task {} from slave {} ({}) due to numOnRack {} and cleaningOnSlave {}", requestId, slaveId, host, countPerRack.count(sanitizedRackId), numCleaningOnSlave);
    return false;
  }

  public void slaveLost(SlaveID slaveIdObj) {
    final String slaveId = slaveIdObj.getValue();

    Optional<SingularitySlave> slave = slaveManager.getObject(slaveId);

    if (slave.isPresent()) {
      MachineState previousState = slave.get().getCurrentState().getState();
      slaveManager.changeState(slave.get(), MachineState.DEAD, Optional.<String> absent(), Optional.<String> absent());
      if (configuration.getDisasterDetection().isEnabled()) {
        updateDisasterCounter(previousState);
      }

      checkRackAfterSlaveLoss(slave.get());
    } else {
      LOG.warn("Lost a slave {}, but didn't know about it", slaveId);
    }
  }

  private void updateDisasterCounter(MachineState previousState) {
    if (previousState == MachineState.ACTIVE) {
      activeSlavesLost.getAndIncrement();
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
      rackManager.changeState(lostSlave.getRackId(), MachineState.DEAD, Optional.<String> absent(), Optional.<String> absent());
    }
  }

  public void loadSlavesAndRacksFromMaster(MesosMasterStateObject state, boolean isStartup) {
    Map<String, SingularitySlave> activeSlavesById = slaveManager.getObjectsByIdForState(MachineState.ACTIVE);
    Map<String, SingularityRack> activeRacksById = rackManager.getObjectsByIdForState(MachineState.ACTIVE);

    Map<String, SingularityRack> remainingActiveRacks = Maps.newHashMap(activeRacksById);

    int slaves = 0;
    int racks = 0;

    for (MesosMasterSlaveObject slaveJsonObject : state.getSlaves()) {
      String slaveId = slaveJsonObject.getId();
      String rackId = slaveAndRackHelper.getRackId(slaveJsonObject.getAttributes());
      Map<String, String> textAttributes = slaveAndRackHelper.getTextAttributes(slaveJsonObject.getAttributes());
      String host = slaveAndRackHelper.getMaybeTruncatedHost(slaveJsonObject.getHostname());

      if (activeSlavesById.containsKey(slaveId)) {
        SingularitySlave slave = activeSlavesById.get(slaveId);
        if (slave != null && (!slave.getResources().isPresent() || !slave.getResources().get().equals(slaveJsonObject.getResources()))) {
          LOG.trace("Found updated resources ({}) for slave {}", slaveJsonObject.getResources(), slave);
          slaveManager.saveObject(slave.withResources(slaveJsonObject.getResources()));
        }
        activeSlavesById.remove(slaveId);
      } else {
        SingularitySlave newSlave = new SingularitySlave(slaveId, host, rackId, textAttributes, Optional.of(slaveJsonObject.getResources()));

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
      slaveManager.changeState(leftOverSlave, isStartup ? MachineState.MISSING_ON_STARTUP : MachineState.DEAD, Optional.<String> absent(), Optional.<String> absent());
    }

    for (SingularityRack leftOverRack : remainingActiveRacks.values()) {
      rackManager.changeState(leftOverRack, isStartup ? MachineState.MISSING_ON_STARTUP : MachineState.DEAD, Optional.<String> absent(), Optional.<String> absent());
    }

    LOG.info("Found {} new racks ({} missing) and {} new slaves ({} missing)", racks, remainingActiveRacks.size(), slaves, activeSlavesById.size());
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
      case FROZEN:
        return CheckResult.ALREADY_ACTIVE;
      case DEAD:
      case MISSING_ON_STARTUP:
        manager.changeState(object.getId(), MachineState.ACTIVE, Optional.<String> absent(), Optional.<String> absent());
        return CheckResult.NEW;
      case DECOMMISSIONED:
      case DECOMMISSIONING:
      case STARTING_DECOMMISSION:
        return CheckResult.DECOMMISSIONING;
    }

    throw new IllegalStateException(String.format("Invalid state %s for %s", currentState, object.getId()));
  }

  @Timed
  public void checkOffer(Offer offer) {
    final String slaveId = offer.getSlaveId().getValue();
    final String rackId = slaveAndRackHelper.getRackIdOrDefault(offer);
    final String host = slaveAndRackHelper.getMaybeTruncatedHost(offer);
    final Map<String, String> textAttributes = slaveAndRackHelper.getTextAttributes(offer);

    final SingularitySlave slave = new SingularitySlave(slaveId, host, rackId, textAttributes, Optional.<MesosResourcesObject>absent());

    if (check(slave, slaveManager) == CheckResult.NEW) {
      LOG.info("Offer revealed a new slave {}", slave);
    }

    final SingularityRack rack = new SingularityRack(rackId);

    if (check(rack, rackManager) == CheckResult.NEW) {
      LOG.info("Offer revealed a new rack {}", rack);
    }
  }

  @Timed
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
        slaveManager.changeState(slave.get(), MachineState.DECOMMISSIONED, slave.get().getCurrentState().getMessage(), slave.get().getCurrentState().getUser());
      }
    }

    Optional<SingularityRack> rack = rackManager.getObject(slave.get().getRackId());

    if (!rack.isPresent()) {
      final String message = String.format("Couldn't find rack with id %s for task %s", slave.get().getRackId(), taskId);
      LOG.warn(message);
      exceptionNotifier.notify(message, ImmutableMap.of("rackId", slave.get().getRackId(), "taskId", taskId.toString()));
      return;
    }

    if (rack.get().getCurrentState().getState() == MachineState.DECOMMISSIONING) {
      if (!hasTaskLeftOnRack(taskId, stateCache)) {
        rackManager.changeState(rack.get(), MachineState.DECOMMISSIONED, rack.get().getCurrentState().getMessage(), rack.get().getCurrentState().getUser());
      }
    }
  }

  private boolean hasTaskLeftOnRack(SingularityTaskId taskId, SingularitySchedulerStateCache stateCache) {
    for (SingularityTaskId activeTaskId : stateCache.getActiveTaskIds()) {
      if (!activeTaskId.equals(taskId) && activeTaskId.getSanitizedRackId().equals(taskId.getSanitizedRackId())) {
        return true;
      }
    }

    return false;
  }

  private boolean hasTaskLeftOnSlave(SingularityTaskId taskId, String slaveId, SingularitySchedulerStateCache stateCache) {
    for (SingularityTaskId activeTaskId : stateCache.getActiveTaskIds()) {
      if (!activeTaskId.equals(taskId) && activeTaskId.getSanitizedHost().equals(taskId.getSanitizedHost())) {
        Optional<SingularityTask> maybeTask = taskManager.getTask(activeTaskId);
        if (maybeTask.isPresent() && slaveId.equals(maybeTask.get().getOffer().getSlaveId().getValue())) {
          return true;
        }
      }
    }

    return false;
  }

}
