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
import com.hubspot.singularity.cache.SingularityCache;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.CuratorAsyncManager;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class UsageManager extends CuratorAsyncManager implements TaskUsageManager {

  private static final String ROOT_PATH = "/usage";

  private static final String SLAVE_PATH = ROOT_PATH + "/slaves";
  private static final String REQUESTS_PATH = ROOT_PATH + "/requests";
  private static final String USAGE_SUMMARY_PATH = ROOT_PATH + "/summary";

  private final Transcoder<SingularitySlaveUsageWithId> slaveUsageTranscoder;
  private final Transcoder<SingularityClusterUtilization> clusterUtilizationTranscoder;
  private final Transcoder<RequestUtilization> requestUtilizationTranscoder;
  private final SingularityCache cache;
  private final TaskUsageManager taskUsageManager;

  @Inject
  public UsageManager(CuratorFramework curator,
                      SingularityConfiguration configuration,
                      MetricRegistry metricRegistry,
                      SingularityCache cache,
                      TaskUsageManager taskUsageManager,
                      Transcoder<SingularitySlaveUsageWithId> slaveUsageTranscoder,
                      Transcoder<SingularityClusterUtilization> clusterUtilizationTranscoder,
                      Transcoder<RequestUtilization> requestUtilizationTranscoder) {
    super(curator, configuration, metricRegistry);
    this.cache = cache;
    this.taskUsageManager = taskUsageManager;
    this.slaveUsageTranscoder = slaveUsageTranscoder;
    this.clusterUtilizationTranscoder = clusterUtilizationTranscoder;
    this.requestUtilizationTranscoder = requestUtilizationTranscoder;
  }

  public void activateLeaderCache() {
    cache.cacheRequestUtilizations(getRequestUtilizations(true));
    cache.cacheSlaveUsages(getAllCurrentSlaveUsage(true));
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

  public Map<String, RequestUtilization> getRequestUtilizations(boolean skipCache) {
    if (!skipCache) {
      return cache.getRequestUtilizations();
    }

    return getAsyncChildren(REQUESTS_PATH, requestUtilizationTranscoder).stream()
        .collect(Collectors.toMap(
            RequestUtilization::getRequestId,
            Function.identity()
        ));
  }

  public Optional<RequestUtilization> getRequestUtilization(String requestId, boolean useWebCache) {
    return Optional.fromNullable(cache.getRequestUtilizations().get(requestId));
  }

  public SingularityCreateResult saveRequestUtilization(RequestUtilization requestUtilization) {
    SingularityCreateResult result = save(getRequestPath(requestUtilization.getRequestId()), requestUtilization, requestUtilizationTranscoder);
    cache.putRequestUtilization(requestUtilization);
    return result;
  }

  public SingularityDeleteResult deleteRequestUtilization(String requestId) {
    SingularityDeleteResult result = delete(getRequestPath(requestId));
    cache.removeRequestUtilization(requestId);
    return result;
  }

  // Slave usages
  private String getSlaveUsagePath(String slaveId) {
    return ZKPaths.makePath(SLAVE_PATH, slaveId);
  }

  public SingularityCreateResult saveCurrentSlaveUsage(SingularitySlaveUsageWithId usageWithId) {
    SingularityCreateResult result = set(getSlaveUsagePath(usageWithId.getSlaveId()), usageWithId, slaveUsageTranscoder);
    cache.putSlaveUsage(usageWithId);
    return result;
  }

  public Optional<SingularitySlaveUsageWithId> getSlaveUsage(String slaveId) {
    return cache.getSlaveUsage(slaveId);
  }

  public Map<String, SingularitySlaveUsageWithId> getAllCurrentSlaveUsage() {
    return getAllCurrentSlaveUsage(false);
  }

  public Map<String, SingularitySlaveUsageWithId> getAllCurrentSlaveUsage(boolean skipCache) {
    if (!skipCache) {
      return cache.getSlaveUsages();
    }
    return getAsyncChildren(SLAVE_PATH, slaveUsageTranscoder).stream()
        .collect(Collectors.toMap(
            SingularitySlaveUsageWithId::getSlaveId,
            Function.identity()
        ));
  }

  public SingularityDeleteResult deleteSlaveUsage(String slaveId) {
    SingularityDeleteResult result = delete(getSlaveUsagePath(slaveId));
    cache.removeSlaveUsage(slaveId);
    return result;
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
