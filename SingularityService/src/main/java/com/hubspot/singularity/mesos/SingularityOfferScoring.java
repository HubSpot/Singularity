package com.hubspot.singularity.mesos;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.AgentMatchState;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityAgentUsage;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.usage.UsageManager;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.mesos.SingularityAgentUsageWithCalculatedScores.MaxProbableUsage;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * It is assumed that all code in this class is run behind a request-level lock
 */
@Singleton
public class SingularityOfferScoring {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityOfferScoring.class
  );

  private final SingularityConfiguration configuration;
  private final MesosConfiguration mesosConfiguration;
  private final SingularityAgentAndRackManager agentAndRackManager;
  private final SingularityAgentAndRackHelper agentAndRackHelper;
  private final DeployManager deployManager;
  private final TaskManager taskManager;
  private final UsageManager usageManager;

  private final double normalizedCpuWeight;
  private final double normalizedMemWeight;
  private final double normalizedDiskWeight;

  @Inject
  public SingularityOfferScoring(
    SingularityConfiguration configuration,
    SingularityAgentAndRackManager agentAndRackManager,
    SingularityAgentAndRackHelper agentAndRackHelper,
    DeployManager deployManager,
    TaskManager taskManager,
    UsageManager usageManager
  ) {
    this.configuration = configuration;
    this.mesosConfiguration = configuration.getMesosConfiguration();
    this.agentAndRackManager = agentAndRackManager;
    this.agentAndRackHelper = agentAndRackHelper;
    this.deployManager = deployManager;
    this.taskManager = taskManager;
    this.usageManager = usageManager;

    double cpuWeight = mesosConfiguration.getCpuWeight();
    double memWeight = mesosConfiguration.getMemWeight();
    double diskWeight = mesosConfiguration.getDiskWeight();
    if (cpuWeight + memWeight + diskWeight != 1) {
      this.normalizedCpuWeight = cpuWeight / (cpuWeight + memWeight + diskWeight);
      this.normalizedMemWeight = memWeight / (cpuWeight + memWeight + diskWeight);
      this.normalizedDiskWeight = diskWeight / (cpuWeight + memWeight + diskWeight);
    } else {
      this.normalizedCpuWeight = cpuWeight;
      this.normalizedMemWeight = memWeight;
      this.normalizedDiskWeight = diskWeight;
    }
  }

  public double score(
    SingularityTaskRequestHolder taskRequestHolder,
    SingularityOfferHolder offerHolder,
    SingularityAgentUsageWithCalculatedScores agentUsage
  ) {
    Map<String, RequestUtilization> requestUtilizations = usageManager.getRequestUtilizations();
    SingularityTaskRequest taskRequest = taskRequestHolder.getTaskRequest();
    final SingularityPendingTaskId pendingTaskId = taskRequest
      .getPendingTask()
      .getPendingTaskId();

    double estimatedCpusToAdd = taskRequestHolder.getTotalResources().getCpus();
    RequestUtilization requestUtilization = requestUtilizations.get(
      taskRequest.getRequest().getId()
    );
    if (requestUtilization != null) {
      estimatedCpusToAdd =
        agentAndRackHelper.getEstimatedCpuUsageForRequest(requestUtilization);
    }
    if (
      mesosConfiguration.isOmitOverloadedHosts() &&
      agentUsage.isCpuOverloaded(estimatedCpusToAdd)
    ) {
      LOG.debug(
        "Agent {} is overloaded (load5 {}/{}, load1 {}/{}, estimated cpus to add: {}, already committed cpus: {}), ignoring offer",
        offerHolder.getHostname(),
        agentUsage.getAgentUsage().getSystemLoad5Min(),
        agentUsage.getAgentUsage().getSystemCpusTotal(),
        agentUsage.getAgentUsage().getSystemLoad1Min(),
        agentUsage.getAgentUsage().getSystemCpusTotal(),
        estimatedCpusToAdd,
        agentUsage.getEstimatedAddedCpusUsage()
      );
      return 0;
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace(
        "Attempting to match task {} resources {} with required role '{}' ({} for task + {} for executor) with remaining offer resources {}",
        pendingTaskId,
        taskRequestHolder.getTotalResources(),
        taskRequest.getRequest().getRequiredRole().orElse("*"),
        taskRequestHolder.getTaskResources(),
        taskRequestHolder.getExecutorResources(),
        MesosUtils.formatForLogging(offerHolder.getCurrentResources())
      );
    }

    final boolean matchesResources = MesosUtils.doesOfferMatchResources(
      taskRequest.getRequest().getRequiredRole(),
      taskRequestHolder.getTotalResources(),
      offerHolder.getCurrentResources(),
      taskRequestHolder.getRequestedPorts()
    );
    if (!matchesResources) {
      return 0;
    }
    final AgentMatchState agentMatchState = agentAndRackManager.doesOfferMatch(
      offerHolder,
      taskRequest,
      taskManager.getActiveTaskIdsForRequest(taskRequest.getRequest().getId()),
      () -> isPreemptibleTask(taskRequest),
      requestUtilization
    );

    if (agentMatchState.isMatchAllowed()) {
      return score(offerHolder.getHostname(), agentUsage, agentMatchState);
    } else if (LOG.isTraceEnabled()) {
      LOG.trace(
        "Ignoring offer on host {} with roles {} on {} for task {}; matched resources: true, agent match state: {}",
        offerHolder.getHostname(),
        offerHolder.getRoles(),
        offerHolder.getHostname(),
        pendingTaskId,
        agentMatchState
      );
    }

    return 0;
  }

  private boolean isPreemptibleTask(SingularityTaskRequest taskRequest) {
    // A long running task can be replaced + killed easily
    if (taskRequest.getRequest().getRequestType().isLongRunning()) {
      return true;
    }

    // A short, non-long-running task
    Optional<SingularityDeployStatistics> deployStatistics = deployManager.getDeployStatistics(
      taskRequest.getRequest().getId(),
      taskRequest.getDeploy().getId()
    );
    return (
      deployStatistics.isPresent() &&
      deployStatistics.get().getAverageRuntimeMillis().isPresent() &&
      deployStatistics.get().getAverageRuntimeMillis().get() <
      configuration.getPreemptibleTaskMaxExpectedRuntimeMs()
    );
  }

  @VisibleForTesting
  double score(
    String hostname,
    SingularityAgentUsageWithCalculatedScores agentUsage,
    AgentMatchState agentMatchState
  ) {
    if (agentUsage == null || agentUsage.isMissingUsageData()) {
      if (mesosConfiguration.isOmitForMissingUsageData()) {
        LOG.info("Skipping agent {} with missing usage data ({})", hostname, agentUsage);
        return 0.0;
      } else {
        LOG.info(
          "Agent {} has missing usage data ({}). Will default to {}",
          hostname,
          agentUsage,
          0.5
        );
        return 0.5;
      }
    }

    double calculatedScore = calculateScore(
      1 - agentUsage.getMemAllocatedScore(),
      agentUsage.getMemInUseScore(),
      1 - agentUsage.getCpusAllocatedScore(),
      agentUsage.getCpusInUseScore(),
      1 - agentUsage.getDiskAllocatedScore(),
      agentUsage.getDiskInUseScore(),
      mesosConfiguration.getInUseResourceWeight(),
      mesosConfiguration.getAllocatedResourceWeight()
    );

    if (agentMatchState == AgentMatchState.PREFERRED_AGENT) {
      LOG.debug(
        "Agent {} is preferred, will scale score by {}",
        hostname,
        configuration.getPreferredAgentScaleFactor()
      );
      calculatedScore *= configuration.getPreferredAgentScaleFactor();
    }

    return calculatedScore;
  }

  private double calculateScore(
    double memAllocatedScore,
    double memInUseScore,
    double cpusAllocatedScore,
    double cpusInUseScore,
    double diskAllocatedScore,
    double diskInUseScore,
    double inUseResourceWeight,
    double allocatedResourceWeight
  ) {
    double score = 0;

    score += (normalizedCpuWeight * allocatedResourceWeight) * cpusAllocatedScore;
    score += (normalizedMemWeight * allocatedResourceWeight) * memAllocatedScore;
    score += (normalizedDiskWeight * allocatedResourceWeight) * diskAllocatedScore;

    score += (normalizedCpuWeight * inUseResourceWeight) * cpusInUseScore;
    score += (normalizedMemWeight * inUseResourceWeight) * memInUseScore;
    score += (normalizedDiskWeight * inUseResourceWeight) * diskInUseScore;

    return score;
  }

  MaxProbableUsage getMaxProbableUsageForAgent(
    String sanitizedHostname,
    Resources defaultResources
  ) {
    Map<String, RequestUtilization> requestUtilizations = usageManager.getRequestUtilizations();
    double cpu = 0;
    double memBytes = 0;
    double diskBytes = 0;
    for (SingularityTaskId taskId : taskManager.getActiveTaskIds()) {
      if (taskId.getSanitizedHost().equals(sanitizedHostname)) {
        if (requestUtilizations.containsKey(taskId.getRequestId())) {
          RequestUtilization utilization = requestUtilizations.get(taskId.getRequestId());
          cpu += agentAndRackHelper.getEstimatedCpuUsageForRequest(utilization);
          memBytes += utilization.getMaxMemBytesUsed();
          diskBytes += utilization.getMaxDiskBytesUsed();
        } else {
          Optional<SingularityTask> maybeTask = taskManager.getTask(taskId);
          if (maybeTask.isPresent()) {
            Resources resources = maybeTask
              .get()
              .getTaskRequest()
              .getPendingTask()
              .getResources()
              .orElse(
                maybeTask
                  .get()
                  .getTaskRequest()
                  .getDeploy()
                  .getResources()
                  .orElse(defaultResources)
              );
            cpu += resources.getCpus();
            memBytes +=
              resources.getMemoryMb() * SingularityAgentUsage.BYTES_PER_MEGABYTE;
            diskBytes += resources.getDiskMb() * SingularityAgentUsage.BYTES_PER_MEGABYTE;
          }
        }
      }
    }
    return new MaxProbableUsage(cpu, memBytes, diskBytes);
  }
}
