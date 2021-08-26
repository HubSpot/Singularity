package com.hubspot.singularity.mesos;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.json.MesosMasterAgentObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.singularity.AgentMatchState;
import com.hubspot.singularity.AgentPlacement;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityAgent;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.AbstractMachineManager;
import com.hubspot.singularity.data.AgentManager;
import com.hubspot.singularity.data.InactiveAgentManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularityAgentAndRackHelper.CpuMemoryPreference;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.Offer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityAgentAndRackManager {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityAgentAndRackManager.class
  );

  private final SingularityConfiguration configuration;

  private final SingularityExceptionNotifier exceptionNotifier;
  private final RackManager rackManager;
  private final AgentManager agentManager;
  private final TaskManager taskManager;
  private final InactiveAgentManager inactiveAgentManager;
  private final SingularityAgentAndRackHelper agentAndRackHelper;
  private final AtomicInteger activeAgentsLost;
  private final SingularityLeaderCache leaderCache;

  @Inject
  SingularityAgentAndRackManager(
    SingularityAgentAndRackHelper agentAndRackHelper,
    SingularityConfiguration configuration,
    SingularityExceptionNotifier exceptionNotifier,
    RackManager rackManager,
    AgentManager agentManager,
    TaskManager taskManager,
    InactiveAgentManager inactiveAgentManager,
    @Named(
      SingularityMesosModule.ACTIVE_AGENTS_LOST_COUNTER
    ) AtomicInteger activeAgentsLost,
    SingularityLeaderCache leaderCache
  ) {
    this.configuration = configuration;

    this.exceptionNotifier = exceptionNotifier;
    this.agentAndRackHelper = agentAndRackHelper;

    this.rackManager = rackManager;
    this.agentManager = agentManager;
    this.taskManager = taskManager;
    this.inactiveAgentManager = inactiveAgentManager;
    this.activeAgentsLost = activeAgentsLost;

    this.leaderCache = leaderCache;
  }

  AgentMatchState doesOfferMatch(
    SingularityOfferHolder offerHolder,
    SingularityTaskRequest taskRequest,
    List<SingularityTaskId> activeTaskIdsForRequest,
    boolean isPreemptibleTask,
    RequestUtilization requestUtilization
  ) {
    final String host = offerHolder.getHostname();
    final String rackId = offerHolder.getRackId();
    final String agentId = offerHolder.getAgentId();

    Optional<SingularityAgent> maybeAgent = agentManager.getObject(agentId);
    if (!maybeAgent.isPresent()) {
      return AgentMatchState.RESOURCES_DO_NOT_MATCH;
    }

    final MachineState currentState = maybeAgent.get().getCurrentState().getState();

    if (currentState == MachineState.FROZEN) {
      return AgentMatchState.AGENT_FROZEN;
    }

    if (currentState.isDecommissioning()) {
      return AgentMatchState.AGENT_DECOMMISSIONING;
    }

    final MachineState currentRackState = rackManager
      .getObject(rackId)
      .get()
      .getCurrentState()
      .getState();

    if (currentRackState == MachineState.FROZEN) {
      return AgentMatchState.RACK_FROZEN;
    }

    if (currentRackState.isDecommissioning()) {
      return AgentMatchState.RACK_DECOMMISSIONING;
    }

    if (
      !taskRequest
        .getRequest()
        .getRackAffinity()
        .orElse(Collections.emptyList())
        .isEmpty()
    ) {
      if (!taskRequest.getRequest().getRackAffinity().get().contains(rackId)) {
        LOG.trace(
          "Task {} requires a rack in {} (current rack {})",
          taskRequest.getPendingTask().getPendingTaskId(),
          taskRequest.getRequest().getRackAffinity().get(),
          rackId
        );
        return AgentMatchState.RACK_AFFINITY_NOT_MATCHING;
      }
    }

    if (!isAttributesMatch(offerHolder, taskRequest, isPreemptibleTask)) {
      return AgentMatchState.AGENT_ATTRIBUTES_DO_NOT_MATCH;
    } else if (
      !areAttributeMinimumsFeasible(offerHolder, taskRequest, activeTaskIdsForRequest)
    ) {
      return AgentMatchState.AGENT_ATTRIBUTES_DO_NOT_MATCH;
    }

    final AgentPlacement agentPlacement = maybeOverrideAgentPlacement(
      taskRequest
        .getRequest()
        .getAgentPlacement()
        .orElse(configuration.getDefaultAgentPlacement())
    );

    if (
      !taskRequest.getRequest().isRackSensitive() &&
      agentPlacement == AgentPlacement.GREEDY
    ) {
      // todo: account for this or let this behavior continue?
      return AgentMatchState.NOT_RACK_OR_AGENT_PARTICULAR;
    }

    final int numDesiredInstances = taskRequest.getRequest().getInstancesSafe();
    boolean allowBounceToSameHost = isAllowBounceToSameHost(taskRequest.getRequest());
    int activeRacksWithCapacityCount = getActiveRacksWithCapacityCount();
    Multiset<String> countPerRack = HashMultiset.create(activeRacksWithCapacityCount);
    double numOnAgent = 0;
    double numCleaningOnAgent = 0;
    double numFromSameBounceOnAgent = 0;
    double numOtherDeploysOnAgent = 0;
    boolean taskLaunchedFromBounceWithActionId =
      taskRequest.getPendingTask().getPendingTaskId().getPendingType() ==
      PendingType.BOUNCE &&
      taskRequest.getPendingTask().getActionId().isPresent();

    final String sanitizedHost = offerHolder.getSanitizedHost();
    final String sanitizedRackId = offerHolder.getSanitizedRackId();
    Collection<SingularityTaskId> cleaningTasks = leaderCache.getCleanupTaskIds();

    for (SingularityTaskId taskId : activeTaskIdsForRequest) {
      if (
        !cleaningTasks.contains(taskId) &&
        !taskManager.isKilledTask(taskId) &&
        taskRequest.getDeploy().getId().equals(taskId.getDeployId())
      ) {
        countPerRack.add(taskId.getSanitizedRackId());
      }

      if (!taskId.getSanitizedHost().equals(sanitizedHost)) {
        continue;
      }

      if (taskRequest.getDeploy().getId().equals(taskId.getDeployId())) {
        if (cleaningTasks.contains(taskId)) {
          numCleaningOnAgent++;
        } else {
          numOnAgent++;
        }
        if (taskLaunchedFromBounceWithActionId) {
          Optional<SingularityTask> maybeTask = taskManager.getTask(taskId);
          boolean errorInTaskData = false;
          if (maybeTask.isPresent()) {
            SingularityPendingTask pendingTask = maybeTask
              .get()
              .getTaskRequest()
              .getPendingTask();
            if (pendingTask.getPendingTaskId().getPendingType() == PendingType.BOUNCE) {
              if (pendingTask.getActionId().isPresent()) {
                if (
                  pendingTask
                    .getActionId()
                    .get()
                    .equals(taskRequest.getPendingTask().getActionId().get())
                ) {
                  numFromSameBounceOnAgent++;
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
        numOtherDeploysOnAgent++;
      }
    }

    if (
      configuration.isAllowRackSensitivity() && taskRequest.getRequest().isRackSensitive()
    ) {
      final boolean isRackOk = isRackOk(
        countPerRack,
        sanitizedRackId,
        numDesiredInstances,
        taskRequest.getRequest().getId(),
        agentId,
        host,
        numCleaningOnAgent
      );

      if (!isRackOk) {
        return AgentMatchState.RACK_SATURATED;
      }
    }

    switch (agentPlacement) {
      case SEPARATE:
      case SEPARATE_BY_DEPLOY:
      case SPREAD_ALL_SLAVES:
      case SPREAD_ALL_AGENTS:
        if (allowBounceToSameHost && taskLaunchedFromBounceWithActionId) {
          if (numFromSameBounceOnAgent > 0) {
            LOG.trace(
              "Rejecting SEPARATE task {} from agent {} ({}) due to numFromSameBounceOnAgent {}",
              taskRequest.getRequest().getId(),
              agentId,
              host,
              numFromSameBounceOnAgent
            );
            return AgentMatchState.AGENT_SATURATED;
          }
        } else {
          if (numOnAgent > 0 || numCleaningOnAgent > 0) {
            LOG.trace(
              "Rejecting {} task {} from agent {} ({}) due to numOnAgent {} numCleaningOnAgent {}",
              agentPlacement.name(),
              taskRequest.getRequest().getId(),
              agentId,
              host,
              numOnAgent,
              numCleaningOnAgent
            );
            return AgentMatchState.AGENT_SATURATED;
          }
        }
        break;
      case SEPARATE_BY_REQUEST:
        if (numOnAgent > 0 || numCleaningOnAgent > 0 || numOtherDeploysOnAgent > 0) {
          LOG.trace(
            "Rejecting SEPARATE_BY_REQUEST task {} from agent {} ({}) due to numOnAgent {} numCleaningOnAgent {} numOtherDeploysOnAgent {}",
            taskRequest.getRequest().getId(),
            agentId,
            host,
            numOnAgent,
            numCleaningOnAgent,
            numOtherDeploysOnAgent
          );
          return AgentMatchState.AGENT_SATURATED;
        }
        break;
      case OPTIMISTIC:
        // If no tasks are active for this request yet, we can fall back to greedy.
        if (activeTaskIdsForRequest.size() > 0) {
          Collection<SingularityPendingTaskId> pendingTasksForRequestClusterwide = leaderCache.getPendingTaskIdsForRequest(
            taskRequest.getRequest().getId()
          );

          Set<String> currentHostsForRequest = activeTaskIdsForRequest
            .stream()
            .map(SingularityTaskId::getSanitizedHost)
            .collect(Collectors.toSet());

          final double numPerAgent =
            activeTaskIdsForRequest.size() / (double) currentHostsForRequest.size();
          final double leniencyCoefficient = configuration.getPlacementLeniency();
          final double threshold =
            numPerAgent *
            (1 + (pendingTasksForRequestClusterwide.size() * leniencyCoefficient));
          final boolean isOk = numOnAgent <= threshold;

          if (!isOk) {
            LOG.trace(
              "Rejecting OPTIMISTIC task {} from agent {} ({}) because numOnAgent {} violates threshold {} (based on active tasks for request {}, current hosts for request {}, pending tasks for request {})",
              taskRequest.getRequest().getId(),
              agentId,
              host,
              numOnAgent,
              threshold,
              activeTaskIdsForRequest.size(),
              currentHostsForRequest.size(),
              pendingTasksForRequestClusterwide.size()
            );
            return AgentMatchState.AGENT_SATURATED;
          }
        }
        break;
      case GREEDY:
    }

    if (isPreferred(offerHolder, taskRequest, requestUtilization)) {
      LOG.debug("Agent {} is preferred", offerHolder.getHostname());
      return AgentMatchState.PREFERRED_AGENT;
    }

    return AgentMatchState.OK;
  }

  private boolean isPreferred(
    SingularityOfferHolder offerHolder,
    SingularityTaskRequest taskRequest,
    RequestUtilization requestUtilization
  ) {
    return (
      isPreferredByAllowedAttributes(offerHolder, taskRequest) ||
      isPreferredByCpuMemory(offerHolder, requestUtilization)
    );
  }

  private AgentPlacement maybeOverrideAgentPlacement(AgentPlacement placement) {
    return configuration.getAgentPlacementOverride().orElse(placement);
  }

  private boolean isPreferredByAllowedAttributes(
    SingularityOfferHolder offerHolder,
    SingularityTaskRequest taskRequest
  ) {
    Map<String, String> allowedAttributes = getAllowedAttributes(taskRequest);
    Map<String, String> hostAttributes = offerHolder.getTextAttributes();
    boolean containsAtLeastOneMatchingAttribute = agentAndRackHelper.containsAtLeastOneMatchingAttribute(
      hostAttributes,
      allowedAttributes
    );
    LOG.trace(
      "is agent {} by allowed attributes? {}",
      offerHolder.getHostname(),
      containsAtLeastOneMatchingAttribute
    );
    return containsAtLeastOneMatchingAttribute;
  }

  public boolean isPreferredByCpuMemory(
    SingularityOfferHolder offerHolder,
    RequestUtilization requestUtilization
  ) {
    if (requestUtilization != null) {
      CpuMemoryPreference cpuMemoryPreference = agentAndRackHelper.getCpuMemoryPreferenceForAgent(
        offerHolder
      );
      CpuMemoryPreference cpuMemoryPreferenceForRequest = agentAndRackHelper.getCpuMemoryPreferenceForRequest(
        requestUtilization
      );
      LOG.trace(
        "CpuMemoryPreference for agent {} is {}, CpuMemoryPreference for request {} is {}",
        offerHolder.getHostname(),
        cpuMemoryPreference.toString(),
        requestUtilization.getRequestId(),
        cpuMemoryPreferenceForRequest.toString()
      );
      return cpuMemoryPreference == cpuMemoryPreferenceForRequest;
    }
    return false;
  }

  private boolean isAttributesMatch(
    SingularityOfferHolder offer,
    SingularityTaskRequest taskRequest,
    boolean isPreemptibleTask
  ) {
    final Map<String, String> requiredAttributes = getRequiredAttributes(taskRequest);
    final Map<String, String> allowedAttributes = getAllowedAttributes(taskRequest);

    if (offer.hasReservedAgentAttributes()) {
      Map<String, String> reservedAgentAttributes = offer.getReservedAgentAttributes();

      Map<String, String> mergedAttributes = new HashMap<>();
      mergedAttributes.putAll(requiredAttributes);
      mergedAttributes.putAll(allowedAttributes);

      if (!mergedAttributes.isEmpty()) {
        if (
          !agentAndRackHelper.containsAllAttributes(
            mergedAttributes,
            reservedAgentAttributes
          )
        ) {
          LOG.trace(
            "Agents with attributes {} are reserved for matching tasks. Task with attributes {} does not match",
            reservedAgentAttributes,
            mergedAttributes
          );
          return false;
        }
      } else {
        LOG.trace(
          "Agents with attributes {} are reserved for matching tasks. No attributes specified for task {}",
          reservedAgentAttributes,
          taskRequest.getPendingTask().getPendingTaskId().getId()
        );
        return false;
      }
    }

    if (!configuration.getPreemptibleTasksOnlyMachineAttributes().isEmpty()) {
      if (
        agentAndRackHelper.containsAllAttributes(
          offer.getTextAttributes(),
          configuration.getPreemptibleTasksOnlyMachineAttributes()
        ) &&
        !isPreemptibleTask
      ) {
        LOG.debug("Host {} is reserved for preemptible tasks", offer.getHostname());
        return false;
      }
    }

    if (!requiredAttributes.isEmpty()) {
      if (
        !agentAndRackHelper.containsAllAttributes(
          offer.getTextAttributes(),
          requiredAttributes
        )
      ) {
        LOG.trace(
          "Task requires agent with attributes {}, (agent attributes are {})",
          requiredAttributes,
          offer.getTextAttributes()
        );
        return false;
      }
    }

    return true;
  }

  private Map<String, String> getRequiredAttributes(SingularityTaskRequest taskRequest) {
    if (!taskRequest.getPendingTask().getRequiredAgentAttributeOverrides().isEmpty()) {
      return taskRequest.getPendingTask().getRequiredAgentAttributeOverrides();
    } else if (
      (
        taskRequest.getRequest().getRequiredAgentAttributes().isPresent() &&
        !taskRequest.getRequest().getRequiredAgentAttributes().get().isEmpty()
      )
    ) {
      return taskRequest.getRequest().getRequiredAgentAttributes().get();
    }
    return new HashMap<>();
  }

  private Map<String, String> getAllowedAttributes(SingularityTaskRequest taskRequest) {
    if (!taskRequest.getPendingTask().getAllowedAgentAttributeOverrides().isEmpty()) {
      return taskRequest.getPendingTask().getAllowedAgentAttributeOverrides();
    } else if (
      (
        taskRequest.getRequest().getAllowedAgentAttributes().isPresent() &&
        !taskRequest.getRequest().getAllowedAgentAttributes().get().isEmpty()
      )
    ) {
      return taskRequest.getRequest().getAllowedAgentAttributes().get();
    }
    return new HashMap<>();
  }

  private boolean areAttributeMinimumsFeasible(
    SingularityOfferHolder offerHolder,
    SingularityTaskRequest taskRequest,
    List<SingularityTaskId> activeTaskIdsForRequest
  ) {
    if (!taskRequest.getRequest().getAgentAttributeMinimums().isPresent()) {
      return true;
    }
    Map<String, String> offerAttributes = agentManager
      .getObject(offerHolder.getAgentId())
      .get()
      .getAttributes();

    Integer numDesiredInstances = taskRequest.getRequest().getInstancesSafe();
    Integer numActiveInstances = activeTaskIdsForRequest.size();

    for (Entry<String, Map<String, Integer>> keyEntry : taskRequest
      .getRequest()
      .getAgentAttributeMinimums()
      .get()
      .entrySet()) {
      String attrKey = keyEntry.getKey();
      for (Entry<String, Integer> valueEntry : keyEntry.getValue().entrySet()) {
        Integer percentInstancesWithAttr = valueEntry.getValue();
        Integer minInstancesWithAttr = Math.max(
          1,
          (int) ((percentInstancesWithAttr / 100.0) * numDesiredInstances)
        );

        if (
          offerAttributes.containsKey(attrKey) &&
          offerAttributes.get(attrKey).equals(valueEntry.getKey())
        ) {
          // Accepting this offer would add an instance of the needed attribute, so it's okay.
          continue;
        }

        // Would accepting this offer prevent meeting the necessary attribute in the future?
        long numInstancesWithAttr = getNumInstancesWithAttribute(
          activeTaskIdsForRequest,
          attrKey,
          valueEntry.getKey()
        );
        long numInstancesWithoutAttr = numActiveInstances - numInstancesWithAttr + 1;

        long maxPotentialInstancesWithAttr =
          numDesiredInstances - numInstancesWithoutAttr;
        if (maxPotentialInstancesWithAttr < minInstancesWithAttr) {
          return false;
        }
      }
    }
    return true;
  }

  private long getNumInstancesWithAttribute(
    List<SingularityTaskId> taskIds,
    String attrKey,
    String attrValue
  ) {
    return taskIds
      .stream()
      .map(
        id ->
          leaderCache
            .getAgent(
              taskManager.getTask(id).get().getMesosTask().getAgentId().getValue()
            )
            .get()
            .getAttributes()
            .get(attrKey)
      )
      .filter(Objects::nonNull)
      .filter(x -> x.equals(attrValue))
      .count();
  }

  private boolean isAllowBounceToSameHost(SingularityRequest request) {
    if (request.getAllowBounceToSameHost().isPresent()) {
      return request.getAllowBounceToSameHost().get();
    } else {
      return configuration.isAllowBounceToSameHost();
    }
  }

  private boolean isRackOk(
    Multiset<String> countPerRack,
    String sanitizedRackId,
    int numDesiredInstances,
    String requestId,
    String agentId,
    String host,
    double numCleaningOnAgent
  ) {
    int racksAccountedFor = countPerRack.elementSet().size();
    int activeRacksWithCapacityCount = getActiveRacksWithCapacityCount();
    double numPerRack = numDesiredInstances / (double) activeRacksWithCapacityCount;
    if (racksAccountedFor < activeRacksWithCapacityCount) {
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

    LOG.trace(
      "Rejecting RackSensitive task {} from agent {} ({}) due to numOnRack {} and cleaningOnAgent {}",
      requestId,
      agentId,
      host,
      countPerRack.count(sanitizedRackId),
      numCleaningOnAgent
    );
    return false;
  }

  void agentLost(AgentID agentIdObj) {
    final String agentId = agentIdObj.getValue();

    Optional<SingularityAgent> agent = agentManager.getObject(agentId);

    if (agent.isPresent()) {
      MachineState previousState = agent.get().getCurrentState().getState();
      agentManager.changeState(
        agent.get(),
        MachineState.DEAD,
        Optional.empty(),
        Optional.empty()
      );
      if (configuration.getDisasterDetection().isEnabled()) {
        updateDisasterCounter(previousState);
      }

      checkRackAfterAgentLoss(agent.get());
    } else {
      LOG.warn("Lost a agent {}, but didn't know about it", agentId);
    }
  }

  private void updateDisasterCounter(MachineState previousState) {
    if (previousState == MachineState.ACTIVE) {
      activeAgentsLost.getAndIncrement();
    }
  }

  private void checkRackAfterAgentLoss(SingularityAgent lostAgent) {
    List<SingularityAgent> agents = agentManager.getObjectsFiltered(MachineState.ACTIVE);

    int numInRack = 0;

    for (SingularityAgent agent : agents) {
      if (agent.getRackId().equals(lostAgent.getRackId())) {
        numInRack++;
      }
    }

    LOG.info("Found {} agents left in rack {}", numInRack, lostAgent.getRackId());

    if (numInRack == 0) {
      rackManager.changeState(
        lostAgent.getRackId(),
        MachineState.DEAD,
        Optional.empty(),
        Optional.empty()
      );
    }
  }

  public void loadAgentsAndRacksFromMaster(
    MesosMasterStateObject state,
    boolean isStartup
  ) {
    Map<String, SingularityAgent> activeAgentsById = agentManager.getObjectsByIdForState(
      MachineState.ACTIVE
    );
    Map<String, SingularityRack> activeRacksById = rackManager.getObjectsByIdForState(
      MachineState.ACTIVE
    );

    Map<String, SingularityRack> remainingActiveRacks = Maps.newHashMap(activeRacksById);

    int agents = 0;
    int racks = 0;

    for (MesosMasterAgentObject agentJsonObject : state.getAgents()) {
      String agentId = agentJsonObject.getId();
      String rackId = agentAndRackHelper.getRackId(agentJsonObject.getAttributes());
      Map<String, String> textAttributes = agentAndRackHelper.getTextAttributes(
        agentJsonObject.getAttributes()
      );
      String host = agentAndRackHelper.getMaybeTruncatedHost(
        agentJsonObject.getHostname()
      );

      if (activeAgentsById.containsKey(agentId)) {
        SingularityAgent agent = activeAgentsById.get(agentId);
        if (
          agent != null &&
          (
            !agent.getResources().isPresent() ||
            !agent.getResources().get().equals(agentJsonObject.getResources())
          )
        ) {
          LOG.trace(
            "Found updated resources ({}) for agent {}",
            agentJsonObject.getResources(),
            agent
          );
          agentManager.saveObject(agent.withResources(agentJsonObject.getResources()));
        }
        activeAgentsById.remove(agentId);
      } else {
        SingularityAgent newAgent = new SingularityAgent(
          agentId,
          host,
          rackId,
          textAttributes,
          Optional.of(agentJsonObject.getResources())
        );

        if (check(newAgent, agentManager) == CheckResult.NEW) {
          agents++;
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

    for (SingularityAgent leftOverAgent : activeAgentsById.values()) {
      agentManager.changeState(
        leftOverAgent,
        isStartup ? MachineState.MISSING_ON_STARTUP : MachineState.DEAD,
        Optional.empty(),
        Optional.empty()
      );
    }

    for (SingularityRack leftOverRack : remainingActiveRacks.values()) {
      rackManager.changeState(
        leftOverRack,
        isStartup ? MachineState.MISSING_ON_STARTUP : MachineState.DEAD,
        Optional.empty(),
        Optional.empty()
      );
    }

    LOG.info(
      "Found {} new racks ({} missing) and {} new agents ({} missing)",
      racks,
      remainingActiveRacks.size(),
      agents,
      activeAgentsById.size()
    );
  }

  public enum CheckResult {
    NEW,
    NOT_ACCEPTING_TASKS,
    ALREADY_ACTIVE
  }

  private <T extends SingularityMachineAbstraction<T>> CheckResult check(
    T object,
    AbstractMachineManager<T> manager
  ) {
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
        manager.changeState(
          object.getId(),
          MachineState.ACTIVE,
          Optional.empty(),
          Optional.empty()
        );
        return CheckResult.NEW;
      case FROZEN:
      case DECOMMISSIONED:
      case DECOMMISSIONING:
      case STARTING_DECOMMISSION:
        return CheckResult.NOT_ACCEPTING_TASKS;
    }

    throw new IllegalStateException(
      String.format("Invalid state %s for %s", currentState, object.getId())
    );
  }

  public CheckResult checkOffer(Offer offer) {
    final String agentId = offer.getAgentId().getValue();
    final String rackId = agentAndRackHelper.getRackIdOrDefault(offer);
    final String host = agentAndRackHelper.getMaybeTruncatedHost(offer);
    final Map<String, String> textAttributes = agentAndRackHelper.getTextAttributes(
      offer
    );

    final SingularityAgent agent = new SingularityAgent(
      agentId,
      host,
      rackId,
      textAttributes,
      Optional.empty()
    );

    CheckResult result = check(agent, agentManager);

    if (result == CheckResult.NEW) {
      if (inactiveAgentManager.isInactive(agent.getHost())) {
        LOG.info(
          "Agent {} on inactive host {} attempted to rejoin. Marking as decommissioned.",
          agent,
          host
        );
        agentManager.changeState(
          agent,
          MachineState.STARTING_DECOMMISSION,
          Optional.of(
            String.format(
              "Agent %s on inactive host %s attempted to rejoin cluster.",
              agentId,
              host
            )
          ),
          Optional.empty()
        );
      } else {
        LOG.info("Offer revealed a new agent {}", agent);
      }
    }

    final SingularityRack rack = new SingularityRack(rackId);

    if (check(rack, rackManager) == CheckResult.NEW) {
      LOG.info("Offer revealed a new rack {}", rack);
    }

    return result;
  }

  void checkStateAfterFinishedTask(
    SingularityTaskId taskId,
    String agentId,
    SingularityLeaderCache leaderCache
  ) {
    Optional<SingularityAgent> agent = agentManager.getObject(agentId);

    if (!agent.isPresent()) {
      final String message = String.format(
        "Couldn't find agent with id %s for task %s",
        agentId,
        taskId
      );
      LOG.warn(message);
      exceptionNotifier.notify(
        message,
        ImmutableMap.of("agentId", agentId, "taskId", taskId.toString())
      );
      return;
    }

    if (agent.get().getCurrentState().getState() == MachineState.DECOMMISSIONING) {
      if (!hasTaskLeftOnAgent(taskId, agentId, leaderCache)) {
        agentManager.changeState(
          agent.get(),
          MachineState.DECOMMISSIONED,
          agent.get().getCurrentState().getMessage(),
          agent.get().getCurrentState().getUser()
        );
      }
    }

    Optional<SingularityRack> rack = rackManager.getObject(agent.get().getRackId());

    if (!rack.isPresent()) {
      final String message = String.format(
        "Couldn't find rack with id %s for task %s",
        agent.get().getRackId(),
        taskId
      );
      LOG.warn(message);
      exceptionNotifier.notify(
        message,
        ImmutableMap.of("rackId", agent.get().getRackId(), "taskId", taskId.toString())
      );
      return;
    }

    if (rack.get().getCurrentState().getState() == MachineState.DECOMMISSIONING) {
      if (!hasTaskLeftOnRack(taskId, leaderCache)) {
        rackManager.changeState(
          rack.get(),
          MachineState.DECOMMISSIONED,
          rack.get().getCurrentState().getMessage(),
          rack.get().getCurrentState().getUser()
        );
      }
    }
  }

  private boolean hasTaskLeftOnRack(
    SingularityTaskId taskId,
    SingularityLeaderCache leaderCache
  ) {
    for (SingularityTaskId activeTaskId : leaderCache.getActiveTaskIds()) {
      if (
        !activeTaskId.equals(taskId) &&
        activeTaskId.getSanitizedRackId().equals(taskId.getSanitizedRackId())
      ) {
        return true;
      }
    }

    return false;
  }

  private boolean hasTaskLeftOnAgent(
    SingularityTaskId taskId,
    String agentId,
    SingularityLeaderCache stateCache
  ) {
    for (SingularityTaskId activeTaskId : stateCache.getActiveTaskIds()) {
      if (
        !activeTaskId.equals(taskId) &&
        activeTaskId.getSanitizedHost().equals(taskId.getSanitizedHost())
      ) {
        Optional<SingularityTask> maybeTask = taskManager.getTask(activeTaskId);
        if (
          maybeTask.isPresent() && agentId.equals(maybeTask.get().getAgentId().getValue())
        ) {
          return true;
        }
      }
    }

    return false;
  }

  public int getActiveRacksWithCapacityCount() {
    return configuration.getExpectedRacksCount().isPresent()
      ? configuration.getExpectedRacksCount().get()
      : rackManager.getNumActive();
  }
}
