package com.hubspot.singularity.data.usage;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityAgentUsageWithId;
import com.hubspot.singularity.SingularityClusterUtilization;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.CuratorAsyncManager;
import com.hubspot.singularity.data.SingularityWebCache;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

@Singleton
public class UsageManager extends CuratorAsyncManager implements TaskUsageManager {

  private static final String ROOT_PATH = "/usage";

  private static final String AGENTS_PATH = ROOT_PATH + "/slaves";
  private static final String REQUESTS_PATH = ROOT_PATH + "/requests";
  private static final String USAGE_SUMMARY_PATH = ROOT_PATH + "/summary";

  private final Transcoder<SingularityAgentUsageWithId> agentUsageTranscoder;
  private final Transcoder<SingularityClusterUtilization> clusterUtilizationTranscoder;
  private final Transcoder<RequestUtilization> requestUtilizationTranscoder;
  private final SingularityWebCache webCache;
  private final SingularityLeaderCache leaderCache;
  private final TaskUsageManager taskUsageManager;

  @Inject
  public UsageManager(
    CuratorFramework curator,
    SingularityConfiguration configuration,
    MetricRegistry metricRegistry,
    SingularityWebCache webCache,
    SingularityLeaderCache leaderCache,
    TaskUsageManager taskUsageManager,
    Transcoder<SingularityAgentUsageWithId> agentUsageTranscoder,
    Transcoder<SingularityClusterUtilization> clusterUtilizationTranscoder,
    Transcoder<RequestUtilization> requestUtilizationTranscoder
  ) {
    super(curator, configuration, metricRegistry);
    this.webCache = webCache;
    this.leaderCache = leaderCache;
    this.taskUsageManager = taskUsageManager;
    this.agentUsageTranscoder = agentUsageTranscoder;
    this.clusterUtilizationTranscoder = clusterUtilizationTranscoder;
    this.requestUtilizationTranscoder = requestUtilizationTranscoder;
  }

  public void activateLeaderCache() {
    leaderCache.cacheRequestUtilizations(fetchRequestUtilizations());
    leaderCache.cacheAgentUsages(fetchAllCurrentAgentUsage());
  }

  public SingularityCreateResult saveClusterUtilization(
    SingularityClusterUtilization utilization
  ) {
    return save(USAGE_SUMMARY_PATH, utilization, clusterUtilizationTranscoder);
  }

  public Optional<SingularityClusterUtilization> getClusterUtilization() {
    return getData(USAGE_SUMMARY_PATH, clusterUtilizationTranscoder);
  }

  // Request utilization
  private String getRequestPath(String requestId) {
    return ZKPaths.makePath(REQUESTS_PATH, requestId);
  }

  public Map<String, RequestUtilization> getRequestUtilizations() {
    return getRequestUtilizations(false);
  }

  public Map<String, RequestUtilization> getRequestUtilizations(boolean useWebCache) {
    if (leaderCache.active()) {
      return leaderCache.getRequestUtilizations();
    }

    if (useWebCache && webCache.useCachedRequestUtilizations()) {
      return webCache.getRequestUtilizations();
    }
    Map<String, RequestUtilization> requestUtilizations = fetchRequestUtilizations();
    if (useWebCache) {
      webCache.cacheRequestUtilizations(requestUtilizations);
    }
    return requestUtilizations;
  }

  private Map<String, RequestUtilization> fetchRequestUtilizations() {
    return getAsyncChildren(REQUESTS_PATH, requestUtilizationTranscoder)
      .stream()
      .collect(Collectors.toMap(RequestUtilization::getRequestId, Function.identity()));
  }

  public Optional<RequestUtilization> getRequestUtilization(
    String requestId,
    boolean useWebCache
  ) {
    if (leaderCache.active()) {
      return Optional.ofNullable(leaderCache.getRequestUtilizations().get(requestId));
    }

    if (useWebCache && webCache.useCachedRequestUtilizations()) {
      return Optional.ofNullable(webCache.getRequestUtilizations().get(requestId));
    }
    return getData(getRequestPath(requestId), requestUtilizationTranscoder);
  }

  public SingularityCreateResult saveRequestUtilization(
    RequestUtilization requestUtilization
  ) {
    if (leaderCache.active()) {
      leaderCache.putRequestUtilization(requestUtilization);
    }
    return save(
      getRequestPath(requestUtilization.getRequestId()),
      requestUtilization,
      requestUtilizationTranscoder
    );
  }

  public SingularityDeleteResult deleteRequestUtilization(String requestId) {
    if (leaderCache.active()) {
      leaderCache.removeRequestUtilization(requestId);
    }
    return delete(getRequestPath(requestId));
  }

  // Agent usages
  private String getAgentUsagePath(String agentId) {
    return ZKPaths.makePath(AGENTS_PATH, agentId);
  }

  public SingularityCreateResult saveCurrentAgentUsage(
    SingularityAgentUsageWithId usageWithId
  ) {
    if (leaderCache.active()) {
      leaderCache.putAgentUsage(usageWithId);
    }
    return set(
      getAgentUsagePath(usageWithId.getAgentId()),
      usageWithId,
      agentUsageTranscoder
    );
  }

  public Optional<SingularityAgentUsageWithId> getAgentUsage(String agentId) {
    if (leaderCache.active()) {
      return leaderCache.getAgentUsage(agentId);
    }
    return getData(getAgentUsagePath(agentId), agentUsageTranscoder);
  }

  public Map<String, SingularityAgentUsageWithId> getAllCurrentAgentUsage() {
    if (leaderCache.active()) {
      return leaderCache.getAgentUsages();
    }
    return fetchAllCurrentAgentUsage();
  }

  private Map<String, SingularityAgentUsageWithId> fetchAllCurrentAgentUsage() {
    return getAsyncChildren(AGENTS_PATH, agentUsageTranscoder)
      .stream()
      .collect(
        Collectors.toMap(SingularityAgentUsageWithId::getAgentId, Function.identity())
      );
  }

  public SingularityDeleteResult deleteAgentUsage(String agentId) {
    if (leaderCache.active()) {
      leaderCache.removeAgentUsage(agentId);
    }
    return delete(getAgentUsagePath(agentId));
  }

  // Task Usage
  public void deleteTaskUsage(SingularityTaskId taskId) {
    taskUsageManager.deleteTaskUsage(taskId);
  }

  public void deleteSpecificTaskUsage(SingularityTaskId taskId, long timestamp) {
    taskUsageManager.deleteSpecificTaskUsage(taskId, timestamp);
  }

  public void saveSpecificTaskUsage(
    SingularityTaskId taskId,
    SingularityTaskUsage usage
  ) {
    taskUsageManager.saveSpecificTaskUsage(taskId, usage);
  }

  public List<SingularityTaskUsage> getTaskUsage(SingularityTaskId taskId) {
    return taskUsageManager.getTaskUsage(taskId);
  }

  public int countTasksWithUsage() {
    return taskUsageManager.countTasksWithUsage();
  }

  public void cleanOldUsages(List<SingularityTaskId> activeTaskIds) {
    taskUsageManager.cleanOldUsages(activeTaskIds);
  }
}
