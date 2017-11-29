package com.hubspot.singularity.mesos;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.Offer;
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
import com.hubspot.mesos.json.MesosMasterSlaveObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlaveMatchState;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.AbstractMachineManager;
import com.hubspot.singularity.data.InactiveSlaveManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularitySlaveAndRackManager {

  private static final Logger LOG = LoggerFactory.getLogger(SingularitySlaveAndRackManager.class);

  private final SingularityConfiguration configuration;

  private final SingularityExceptionNotifier exceptionNotifier;
  private final RackManager rackManager;
  private final SlaveManager slaveManager;
  private final TaskManager taskManager;
  private final InactiveSlaveManager inactiveSlaveManager;
  private final SingularitySlaveAndRackHelper slaveAndRackHelper;
  private final AtomicInteger activeSlavesLost;
  private final SingularityLeaderCache leaderCache;

  @Inject
  SingularitySlaveAndRackManager(SingularitySlaveAndRackHelper slaveAndRackHelper, SingularityConfiguration configuration, SingularityExceptionNotifier exceptionNotifier,
                                 RackManager rackManager, SlaveManager slaveManager, TaskManager taskManager, InactiveSlaveManager inactiveSlaveManager,
                                 @Named(SingularityMesosModule.ACTIVE_SLAVES_LOST_COUNTER) AtomicInteger activeSlavesLost, SingularityLeaderCache leaderCache) {
    this.configuration = configuration;

    this.exceptionNotifier = exceptionNotifier;
    this.slaveAndRackHelper = slaveAndRackHelper;

    this.rackManager = rackManager;
    this.slaveManager = slaveManager;
    this.taskManager = taskManager;
    this.inactiveSlaveManager = inactiveSlaveManager;
    this.activeSlavesLost = activeSlavesLost;

    this.leaderCache = leaderCache;
  }

  public SlaveMatchState doesOfferMatch(SingularityOfferHolder offerHolder, SingularityTaskRequest taskRequest) {
    final String host = offerHolder.getHostname();
    final String rackId = offerHolder.getRackId();
    final String slaveId = offerHolder.getSlaveId();

    final MachineState currentSlaveState = slaveManager.getSlave(slaveId).get().getCurrentState().getState();

    if (currentSlaveState == MachineState.FROZEN) {
      return SlaveMatchState.SLAVE_FROZEN;
    }

    if (currentSlaveState.isDecommissioning()) {
      return SlaveMatchState.SLAVE_DECOMMISSIONING;
    }

    final MachineState currentRackState = rackManager.getRack(rackId).get().getCurrentState().getState();

    if (currentRackState == MachineState.FROZEN) {
      return SlaveMatchState.RACK_FROZEN;
    }

    if (currentRackState.isDecommissioning()) {
      return SlaveMatchState.RACK_DECOMMISSIONING;
    }

    if (!taskRequest.getRequest().getRackAffinity().or(Collections.emptyList()).isEmpty()) {
      if (!taskRequest.getRequest().getRackAffinity().get().contains(rackId)) {
        LOG.trace("Task {} requires a rack in {} (current rack {})", taskRequest.getPendingTask().getPendingTaskId(), taskRequest.getRequest().getRackAffinity().get(), rackId);
        return SlaveMatchState.RACK_AFFINITY_NOT_MATCHING;
      }
    }

    if (!isSlaveAttributesMatch(offerHolder, taskRequest)) {
      return SlaveMatchState.SLAVE_ATTRIBUTES_DO_NOT_MATCH;
    }

    final SlavePlacement slavePlacement = taskRequest.getRequest().getSlavePlacement().or(configuration.getDefaultSlavePlacement());

    if (!taskRequest.getRequest().isRackSensitive() && slavePlacement == SlavePlacement.GREEDY) {
      // todo: account for this or let this behavior continue?
      return SlaveMatchState.NOT_RACK_OR_SLAVE_PARTICULAR;
    }

    final int numDesiredInstances = taskRequest.getRequest().getInstancesSafe();
    boolean allowBounceToSameHost = isAllowBounceToSameHost(taskRequest.getRequest());
    Multiset<String> countPerRack = HashMultiset.create(slaveManager.getNumActive());
    double numOnSlave = 0;
    double numCleaningOnSlave = 0;
    double numFromSameBounceOnSlave = 0;
    double numOtherDeploysOnSlave = 0;
    boolean taskLaunchedFromBounceWithActionId = taskRequest.getPendingTask().getPendingTaskId().getPendingType() == PendingType.BOUNCE && taskRequest.getPendingTask().getActionId().isPresent();

    final String sanitizedHost = offerHolder.getSanitizedHost();
    final String sanitizedRackId = offerHolder.getSanitizedRackId();
    Collection<SingularityTaskId> cleaningTasks = leaderCache.getCleanupTaskIds();

    for (SingularityTaskId taskId : leaderCache.getActiveTaskIdsForRequest(taskRequest.getRequest().getId())) {
      // TODO consider using executorIds

      if (!cleaningTasks.contains(taskId) && taskRequest.getDeploy().getId().equals(taskId.getDeployId())) {
        countPerRack.add(taskId.getSanitizedRackId());
      }

      if (!taskId.getSanitizedHost().equals(sanitizedHost)) {
        continue;
      }

      if (taskRequest.getDeploy().getId().equals(taskId.getDeployId())) {
        if (cleaningTasks.contains(taskId)) {
          numCleaningOnSlave++;
        } else {
          numOnSlave++;
        }
        if (taskLaunchedFromBounceWithActionId) {
          Optional<SingularityTask> maybeTask = taskManager.getTask(taskId);
          boolean errorInTaskData = false;
          if (maybeTask.isPresent()) {
            SingularityPendingTask pendingTask = maybeTask.get().getTaskRequest().getPendingTask();
            if (pendingTask.getPendingTaskId().getPendingType() == PendingType.BOUNCE) {
              if (pendingTask.getActionId().isPresent()) {
                if (pendingTask.getActionId().get().equals(taskRequest.getPendingTask().getActionId().get())) {
                  numFromSameBounceOnSlave++;
                }
              } else {
                // No actionId present on bounce, fall back to more restrictive placement strategy
                errorInTaskData = true;
              }
            }
          } else {
            // Could not find appropriate task data, fall back to more restrictive placement strategy
            errorInTaskData = true;
          }
          if (errorInTaskData) {
            allowBounceToSameHost = false;
          }
        }
      } else {
        numOtherDeploysOnSlave++;
      }
    }

    if (taskRequest.getRequest().isRackSensitive()) {
      final boolean isRackOk = isRackOk(countPerRack, sanitizedRackId, numDesiredInstances, taskRequest.getRequest().getId(), slaveId, host, numCleaningOnSlave, leaderCache);

      if (!isRackOk) {
        return SlaveMatchState.RACK_SATURATED;
      }
    }

    switch (slavePlacement) {
      case SEPARATE:
      case SEPARATE_BY_DEPLOY:
      case SPREAD_ALL_SLAVES:
        if (allowBounceToSameHost && taskLaunchedFromBounceWithActionId) {
          if (numFromSameBounceOnSlave > 0) {
            LOG.trace("Rejecting SEPARATE task {} from slave {} ({}) due to numFromSameBounceOnSlave {}", taskRequest.getRequest().getId(), slaveId, host, numFromSameBounceOnSlave);
            return SlaveMatchState.SLAVE_SATURATED;
          }
        } else {
          if (numOnSlave > 0 || numCleaningOnSlave > 0) {
            LOG.trace("Rejecting {} task {} from slave {} ({}) due to numOnSlave {} numCleaningOnSlave {}", slavePlacement.name(), taskRequest.getRequest().getId(), slaveId, host, numOnSlave, numCleaningOnSlave);
            return SlaveMatchState.SLAVE_SATURATED;
          }
        }
        break;
      case SEPARATE_BY_REQUEST:
        if (numOnSlave > 0 || numCleaningOnSlave > 0 || numOtherDeploysOnSlave > 0) {
          LOG.trace("Rejecting SEPARATE_BY_REQUEST task {} from slave {} ({}) due to numOnSlave {} numCleaningOnSlave {} numOtherDeploysOnSlave {}", taskRequest.getRequest().getId(), slaveId, host, numOnSlave, numCleaningOnSlave, numOtherDeploysOnSlave);
          return SlaveMatchState.SLAVE_SATURATED;
        }
        break;
      case OPTIMISTIC:
        Collection<SingularityTaskId> currentlyActiveTasksForRequestClusterwide = leaderCache.getActiveTaskIdsForRequest(taskRequest.getRequest().getId());

        // If no tasks are active for this request yet, we can fall back to greedy.
        if (currentlyActiveTasksForRequestClusterwide.size() > 0) {
          Collection<SingularityPendingTaskId> pendingTasksForRequestClusterwide = leaderCache.getPendingTaskIdsForRequest(taskRequest.getRequest().getId());

          Set<String> currentHostsForRequest = currentlyActiveTasksForRequestClusterwide.stream()
              .map(SingularityTaskId::getSanitizedHost)
              .collect(Collectors.toSet());

          final double numPerSlave = currentlyActiveTasksForRequestClusterwide.size() / (double) currentHostsForRequest.size();
          final double leniencyCoefficient = configuration.getPlacementLeniency();
          final double threshold = numPerSlave * (1 + (pendingTasksForRequestClusterwide.size() * leniencyCoefficient));
          final boolean isSlaveOk = numOnSlave <= threshold;

          if (!isSlaveOk) {
            LOG.trace(
                "Rejecting OPTIMISTIC task {} from slave {} ({}) because numOnSlave {} violates threshold {} (based on active tasks for request {}, current hosts for request {}, pending tasks for request {})",
                taskRequest.getRequest().getId(), slaveId, host, numOnSlave, threshold, currentlyActiveTasksForRequestClusterwide.size(), currentHostsForRequest.size(), pendingTasksForRequestClusterwide.size()
            );
            return SlaveMatchState.SLAVE_SATURATED;
          }
        }
        break;
      case GREEDY:
    }

    return SlaveMatchState.OK;
  }

  private boolean isSlaveAttributesMatch(SingularityOfferHolder offer, SingularityTaskRequest taskRequest) {
    if (offer.hasReservedSlaveAttributes()) {
      Map<String, String> reservedSlaveAttributes = offer.getReservedSlaveAttributes();

      if ((taskRequest.getRequest().getRequiredSlaveAttributes().isPresent() && !taskRequest.getRequest().getRequiredSlaveAttributes().get().isEmpty())
          || (taskRequest.getRequest().getAllowedSlaveAttributes().isPresent() && !taskRequest.getRequest().getAllowedSlaveAttributes().get().isEmpty())) {
        Map<String, String> mergedAttributes = new HashMap<>();
        mergedAttributes.putAll(taskRequest.getRequest().getRequiredSlaveAttributes().or(new HashMap<>()));
        mergedAttributes.putAll(taskRequest.getRequest().getAllowedSlaveAttributes().or(new HashMap<>()));
        if (!slaveAndRackHelper.hasRequiredAttributes(mergedAttributes, reservedSlaveAttributes)) {
          LOG.trace("Slaves with attributes {} are reserved for matching tasks. Task with attributes {} does not match", reservedSlaveAttributes, taskRequest.getRequest().getRequiredSlaveAttributes().or(Collections.emptyMap()));
          return false;
        }
      } else {
        LOG.trace("Slaves with attributes {} are reserved for matching tasks. No attributes specified for task {}", reservedSlaveAttributes, taskRequest.getPendingTask().getPendingTaskId().getId());
        return false;
      }
    }

    if (taskRequest.getRequest().getRequiredSlaveAttributes().isPresent()
        && !slaveAndRackHelper.hasRequiredAttributes(offer.getTextAttributes(), taskRequest.getRequest().getRequiredSlaveAttributes().get())) {
      LOG.trace("Task requires slave with attributes {}, (slave attributes are {})", taskRequest.getRequest().getRequiredSlaveAttributes().get(), offer.getTextAttributes());
      return false;
    }

    return true;
  }

  private boolean isAllowBounceToSameHost(SingularityRequest request) {
    if (request.getAllowBounceToSameHost().isPresent()) {
      return request.getAllowBounceToSameHost().get();
    } else {
      return configuration.isAllowBounceToSameHost();
    }
  }

  private boolean isRackOk(Multiset<String> countPerRack, String sanitizedRackId, int numDesiredInstances, String requestId, String slaveId, String host, double numCleaningOnSlave, SingularityLeaderCache leaderCache) {
    int racksAccountedFor = countPerRack.elementSet().size();
    double numPerRack = numDesiredInstances / (double) rackManager.getNumActive();
    if (racksAccountedFor < rackManager.getNumActive()) {
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

  public void slaveLost(AgentID slaveIdObj) {
    final String slaveId = slaveIdObj.getValue();

    Optional<SingularitySlave> slave = slaveManager.getObject(slaveId);

    if (slave.isPresent()) {
      MachineState previousState = slave.get().getCurrentState().getState();
      slaveManager.changeState(slave.get(), MachineState.DEAD, Optional.absent(), Optional.absent());
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
      rackManager.changeState(lostSlave.getRackId(), MachineState.DEAD, Optional.absent(), Optional.absent());
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
      slaveManager.changeState(leftOverSlave, isStartup ? MachineState.MISSING_ON_STARTUP : MachineState.DEAD, Optional.absent(), Optional.absent());
    }

    for (SingularityRack leftOverRack : remainingActiveRacks.values()) {
      rackManager.changeState(leftOverRack, isStartup ? MachineState.MISSING_ON_STARTUP : MachineState.DEAD, Optional.absent(), Optional.absent());
    }

    LOG.info("Found {} new racks ({} missing) and {} new slaves ({} missing)", racks, remainingActiveRacks.size(), slaves, activeSlavesById.size());
  }

  public enum CheckResult {
    NEW, NOT_ACCEPTING_TASKS, ALREADY_ACTIVE;
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
        manager.changeState(object.getId(), MachineState.ACTIVE, Optional.absent(), Optional.absent());
        return CheckResult.NEW;
      case FROZEN:
      case DECOMMISSIONED:
      case DECOMMISSIONING:
      case STARTING_DECOMMISSION:
        return CheckResult.NOT_ACCEPTING_TASKS;
    }

    throw new IllegalStateException(String.format("Invalid state %s for %s", currentState, object.getId()));
  }

  @Timed
  public CheckResult checkOffer(Offer offer) {
    final String slaveId = offer.getAgentId().getValue();
    final String rackId = slaveAndRackHelper.getRackIdOrDefault(offer);
    final String host = slaveAndRackHelper.getMaybeTruncatedHost(offer);
    final Map<String, String> textAttributes = slaveAndRackHelper.getTextAttributes(offer);

    final SingularitySlave slave = new SingularitySlave(slaveId, host, rackId, textAttributes, Optional.absent());

    CheckResult result = check(slave, slaveManager);

    if (result == CheckResult.NEW) {
      if (inactiveSlaveManager.isInactive(slave.getHost())) {
        LOG.info("Slave {} on inactive host {} attempted to rejoin. Marking as decommissioned.", slave, host);
        slaveManager.changeState(slave, MachineState.STARTING_DECOMMISSION,
            Optional.of(String.format("Slave %s on inactive host %s attempted to rejoin cluster.", slaveId, host)),
            Optional.absent());
      } else {
        LOG.info("Offer revealed a new slave {}", slave);
      }
    }

    final SingularityRack rack = new SingularityRack(rackId);

    if (check(rack, rackManager) == CheckResult.NEW) {
      LOG.info("Offer revealed a new rack {}", rack);
    }

    return result;
  }

  @Timed
  public void checkStateAfterFinishedTask(SingularityTaskId taskId, String slaveId, SingularityLeaderCache leaderCache) {
    Optional<SingularitySlave> slave = slaveManager.getSlave(slaveId);

    if (!slave.isPresent()) {
      final String message = String.format("Couldn't find slave with id %s for task %s", slaveId, taskId);
      LOG.warn(message);
      exceptionNotifier.notify(message, ImmutableMap.of("slaveId", slaveId, "taskId", taskId.toString()));
      return;
    }

    if (slave.get().getCurrentState().getState() == MachineState.DECOMMISSIONING) {
      if (!hasTaskLeftOnSlave(taskId, slaveId, leaderCache)) {
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
      if (!hasTaskLeftOnRack(taskId, leaderCache)) {
        rackManager.changeState(rack.get(), MachineState.DECOMMISSIONED, rack.get().getCurrentState().getMessage(), rack.get().getCurrentState().getUser());
      }
    }
  }

  private boolean hasTaskLeftOnRack(SingularityTaskId taskId, SingularityLeaderCache leaderCache) {
    for (SingularityTaskId activeTaskId : leaderCache.getActiveTaskIds()) {
      if (!activeTaskId.equals(taskId) && activeTaskId.getSanitizedRackId().equals(taskId.getSanitizedRackId())) {
        return true;
      }
    }

    return false;
  }

  private boolean hasTaskLeftOnSlave(SingularityTaskId taskId, String slaveId, SingularityLeaderCache stateCache) {
    for (SingularityTaskId activeTaskId : stateCache.getActiveTaskIds()) {
      if (!activeTaskId.equals(taskId) && activeTaskId.getSanitizedHost().equals(taskId.getSanitizedHost())) {
        Optional<SingularityTask> maybeTask = taskManager.getTask(activeTaskId);
        if (maybeTask.isPresent() && slaveId.equals(maybeTask.get().getAgentId().getValue())) {
          return true;
        }
      }
    }

    return false;
  }

}
