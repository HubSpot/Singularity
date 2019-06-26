package com.hubspot.singularity.data.usage;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityClusterUtilization;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.CuratorAsyncManager;
import com.hubspot.singularity.data.SingularityWebCache;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;

@Singleton
public class UsageManager extends CuratorAsyncManager implements TaskUsageManager {

  private static final String ROOT_PATH = "/usage";

  private static final String SLAVE_PATH = ROOT_PATH + "/slaves";
  private static final String REQUESTS_PATH = ROOT_PATH + "/requests";
  private static final String USAGE_SUMMARY_PATH = ROOT_PATH + "/summary";

  private final Transcoder<SingularitySlaveUsageWithId> slaveUsageTranscoder;
  private final Transcoder<SingularityClusterUtilization> clusterUtilizationTranscoder;
  private final Transcoder<RequestUtilization> requestUtilizationTranscoder;
  private final SingularityWebCache webCache;
  private final SingularityLeaderCache leaderCache;
  private final TaskUsageManager taskUsageManager;

  @Inject
  public UsageManager(CuratorFramework curator,
                      SingularityConfiguration configuration,
                      MetricRegistry metricRegistry,
                      SingularityWebCache webCache,
                      SingularityLeaderCache leaderCache,
                      TaskUsageManager taskUsageManager,
                      Transcoder<SingularitySlaveUsageWithId> slaveUsageTranscoder,
                      Transcoder<SingularityClusterUtilization> clusterUtilizationTranscoder,
                      Transcoder<RequestUtilization> requestUtilizationTranscoder) {
    super(curator, configuration, metricRegistry);
    this.webCache = webCache;
    this.leaderCache = leaderCache;
    this.taskUsageManager = taskUsageManager;
    this.slaveUsageTranscoder = slaveUsageTranscoder;
    this.clusterUtilizationTranscoder = clusterUtilizationTranscoder;
    this.requestUtilizationTranscoder = requestUtilizationTranscoder;
  }

  public void activateLeaderCache() {
    leaderCache.cacheRequestUtilizations(getRequestUtilizations(false));
    leaderCache.cacheSlaveUsages(getAllCurrentSlaveUsage());
  }

  public SingularityCreateResult saveClusterUtilization(SingularityClusterUtilization utilization) {
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
    Map<String, RequestUtilization> requestUtilizations = getAsyncChildren(REQUESTS_PATH, requestUtilizationTranscoder).stream()
        .collect(Collectors.toMap(
            RequestUtilization::getRequestId,
            Function.identity()
        ));
    if (useWebCache) {
      webCache.cacheRequestUtilizations(requestUtilizations);
    }
    return requestUtilizations;
  }

  public Optional<RequestUtilization> getRequestUtilization(String requestId, boolean useWebCache) {
    if (leaderCache.active()) {
      return Optional.fromNullable(leaderCache.getRequestUtilizations().get(requestId));
    }

    if (useWebCache && webCache.useCachedRequestUtilizations()) {
      return Optional.fromNullable(webCache.getRequestUtilizations().get(requestId));
    }
    return getData(getRequestPath(requestId), requestUtilizationTranscoder);
  }

  public SingularityCreateResult saveRequestUtilization(RequestUtilization requestUtilization) {
    if (leaderCache.active()) {
      leaderCache.putRequestUtilization(requestUtilization);
    }
    return save(getRequestPath(requestUtilization.getRequestId()), requestUtilization, requestUtilizationTranscoder);
  }

  public SingularityDeleteResult deleteRequestUtilization(String requestId) {
    if (leaderCache.active()) {
      leaderCache.removeRequestUtilization(requestId);
    }
    return delete(getRequestPath(requestId));
  }

  // Slave usages
  private String getSlaveUsagePath(String slaveId) {
    return ZKPaths.makePath(SLAVE_PATH, slaveId);
  }

  public SingularityCreateResult saveCurrentSlaveUsage(SingularitySlaveUsageWithId usageWithId) {
    if (leaderCache.active()) {
      leaderCache.putSlaveUsage(usageWithId);
    }
    return set(getSlaveUsagePath(usageWithId.getSlaveId()), usageWithId, slaveUsageTranscoder);
  }

  public Optional<SingularitySlaveUsageWithId> getSlaveUsage(String slaveId) {
    if (leaderCache.active()) {
      return leaderCache.getSlaveUsage(slaveId);
    }
    return getData(getSlaveUsagePath(slaveId), slaveUsageTranscoder);
  }

  public Map<String, SingularitySlaveUsageWithId> getAllCurrentSlaveUsage() {
    if (leaderCache.active()) {
      return leaderCache.getSlaveUsages();
    }
    return getAsyncChildren(SLAVE_PATH, slaveUsageTranscoder).stream()
        .collect(Collectors.toMap(
            SingularitySlaveUsageWithId::getSlaveId,
            Function.identity()
        ));
  }

  public SingularityDeleteResult deleteSlaveUsage(String slaveId) {
    if (leaderCache.active()) {
      leaderCache.removeSlaveUsage(slaveId);
    }
    return delete(getSlaveUsagePath(slaveId));
  }

  // Task Usage
  public void deleteTaskUsage(SingularityTaskId taskId) {
    taskUsageManager.deleteTaskUsage(taskId);
  }

  public void deleteSpecificTaskUsage(SingularityTaskId taskId, long timestamp) {
    taskUsageManager.deleteSpecificTaskUsage(taskId, timestamp);
  }

  public void saveSpecificTaskUsage(SingularityTaskId taskId, SingularityTaskUsage usage) {
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
