package com.hubspot.singularity.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTaskCurrentUsage;
import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;

@Singleton
public class UsageManager extends CuratorAsyncManager {

  private static final String ROOT_PATH = "/usage";

  private static final String SLAVE_PATH = ROOT_PATH + "/slaves";
  private static final String TASK_PATH = ROOT_PATH + "/tasks";
  private static final String REQUESTS_PATH = ROOT_PATH + "/requests";
  private static final String USAGE_SUMMARY_PATH = ROOT_PATH + "/summary";

  private static final String USAGE_HISTORY_PATH_KEY = "history";
  private static final String CURRENT_USAGE_NODE_KEY = "CURRENT";

  private final Transcoder<SingularitySlaveUsage> slaveUsageTranscoder;
  private final Transcoder<SingularityTaskUsage> taskUsageTranscoder;
  private final Transcoder<SingularityTaskCurrentUsage> taskCurrentUsageTranscoder;
  private final Transcoder<SingularityClusterUtilization> clusterUtilizationTranscoder;
  private final Transcoder<RequestUtilization> requestUtilizationTranscoder;
  private final SingularityWebCache webCache;
  private final SingularityLeaderCache leaderCache;

  @Inject
  public UsageManager(CuratorFramework curator,
                      SingularityConfiguration configuration,
                      MetricRegistry metricRegistry,
                      SingularityWebCache webCache,
                      SingularityLeaderCache leaderCache,
                      Transcoder<SingularitySlaveUsage> slaveUsageTranscoder,
                      Transcoder<SingularityTaskUsage> taskUsageTranscoder,
                      Transcoder<SingularityTaskCurrentUsage> taskCurrentUsageTranscoder,
                      Transcoder<SingularityClusterUtilization> clusterUtilizationTranscoder,
                      Transcoder<RequestUtilization> requestUtilizationTranscoder) {
    super(curator, configuration, metricRegistry);
    this.webCache = webCache;
    this.leaderCache = leaderCache;
    this.slaveUsageTranscoder = slaveUsageTranscoder;
    this.taskUsageTranscoder = taskUsageTranscoder;
    this.taskCurrentUsageTranscoder = taskCurrentUsageTranscoder;
    this.clusterUtilizationTranscoder = clusterUtilizationTranscoder;
    this.requestUtilizationTranscoder = requestUtilizationTranscoder;
  }

  public List<String> getSlavesWithUsage() {
    return getChildren(SLAVE_PATH);
  }

  public int getNumSlavesWithUsage() {
    return getNumChildren(SLAVE_PATH);
  }
  public List<String> getTasksWithUsage() {
    return getChildren(TASK_PATH);
  }

  // /slaves/<slaveid>/CURRENT
  private String getSlaveIdFromCurrentUsagePath(String path) {
    return path.substring(path.indexOf(SLAVE_PATH) + SLAVE_PATH.length() + 1, path.lastIndexOf("/"));
  }

  // /tasks/<taskid>/CURRENT
  private String getTaskIdFromCurrentUsagePath(String path) {
    return path.substring(path.indexOf(TASK_PATH) + TASK_PATH.length() + 1, path.lastIndexOf("/"));
  }

  private String getSlaveUsagePath(String slaveId) {
    return ZKPaths.makePath(SLAVE_PATH, slaveId);
  }

  private String getTaskUsagePath(String taskId) {
    return ZKPaths.makePath(TASK_PATH, taskId);
  }

  private String getSlaveUsageHistoryPath(String slaveId) {
    return ZKPaths.makePath(getSlaveUsagePath(slaveId), USAGE_HISTORY_PATH_KEY);
  }

  private String getTaskUsageHistoryPath(String taskId) {
    return ZKPaths.makePath(getTaskUsagePath(taskId), USAGE_HISTORY_PATH_KEY);
  }

  private String getSpecificSlaveUsagePath(String slaveId, long timestamp) {
    return ZKPaths.makePath(getSlaveUsageHistoryPath(slaveId), Long.toString(timestamp));
  }

  private String getSpecificTaskUsagePath(String taskId, double timestamp) {
    return ZKPaths.makePath(getTaskUsageHistoryPath(taskId), Double.toString(timestamp));
  }

  private String getCurrentSlaveUsagePath(String slaveId) {
    return ZKPaths.makePath(getSlaveUsagePath(slaveId), CURRENT_USAGE_NODE_KEY);
  }

  private String getCurrentTaskUsagePath(String taskId) {
    return ZKPaths.makePath(getTaskUsagePath(taskId), CURRENT_USAGE_NODE_KEY);
  }

  private String getRequestPath(String requestId) {
    return ZKPaths.makePath(REQUESTS_PATH, requestId);
  }

  public SingularityDeleteResult deleteSlaveUsage(String slaveId) {
    return delete(getSlaveUsagePath(slaveId));
  }

  public SingularityDeleteResult deleteTaskUsage(String taskId) {
    return delete(getTaskUsagePath(taskId));
  }

  public SingularityDeleteResult deleteSpecificSlaveUsage(String slaveId, long timestamp) {
    return delete(getSpecificSlaveUsagePath(slaveId, timestamp));
  }

  public SingularityDeleteResult deleteSpecificTaskUsage(String taskId, double timestamp) {
    return delete(getSpecificTaskUsagePath(taskId, timestamp));
  }

  public SingularityCreateResult saveCurrentTaskUsage(String taskId, SingularityTaskCurrentUsage usage) {
    return set(getCurrentTaskUsagePath(taskId), usage, taskCurrentUsageTranscoder);
  }

  public SingularityCreateResult saveSpecificTaskUsage(String taskId, SingularityTaskUsage usage) {
    return save(getSpecificTaskUsagePath(taskId, usage.getTimestamp()), usage, taskUsageTranscoder);
  }

  public SingularityCreateResult saveClusterUtilization(SingularityClusterUtilization utilization) {
    return save(USAGE_SUMMARY_PATH, utilization, clusterUtilizationTranscoder);
  }

  public SingularityCreateResult saveSpecificSlaveUsageAndSetCurrent(String slaveId, SingularitySlaveUsage usage) {
    set(getCurrentSlaveUsagePath(slaveId), usage, slaveUsageTranscoder);
    return save(getSpecificSlaveUsagePath(slaveId, usage.getTimestamp()), usage, slaveUsageTranscoder);
  }

  private static final Comparator<SingularitySlaveUsage> SLAVE_USAGE_COMPARATOR_TIMESTAMP_ASC = Comparator.comparingLong(SingularitySlaveUsage::getTimestamp);

  public List<SingularitySlaveUsage> getSlaveUsage(String slaveId) {
    List<SingularitySlaveUsage> children = getAsyncChildren(getSlaveUsageHistoryPath(slaveId), slaveUsageTranscoder);
    children.sort(SLAVE_USAGE_COMPARATOR_TIMESTAMP_ASC);
    return children;
  }

  private static final Comparator<SingularityTaskUsage> TASK_USAGE_COMPARATOR_TIMESTAMP_ASC = Comparator.comparingDouble(SingularityTaskUsage::getTimestamp);

  public List<SingularityTaskUsage> getTaskUsage(String taskId) {
    List<SingularityTaskUsage> children = getAsyncChildren(getTaskUsageHistoryPath(taskId), taskUsageTranscoder);
    children.sort(TASK_USAGE_COMPARATOR_TIMESTAMP_ASC);
    return children;
  }

  private static final Comparator<String> TASK_USAGE_PATH_COMPARATOR_TIMESTAMP_ASC = Comparator.comparingDouble(Double::parseDouble);

  public List<String> getTaskUsagePaths(String taskId) {
    List<String> children = getChildren(getTaskUsageHistoryPath(taskId));
    children.sort(TASK_USAGE_PATH_COMPARATOR_TIMESTAMP_ASC);
    return children;
  }

  public Optional<SingularityClusterUtilization> getClusterUtilization() {
    return getData(USAGE_SUMMARY_PATH, clusterUtilizationTranscoder);
  }

  public void activateLeaderCache() {
    leaderCache.cacheRequestUtilizations(getRequestUtilizations(false));
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

  public List<SingularitySlaveUsageWithId> getCurrentSlaveUsages(List<String> slaveIds) {
    List<String> paths = new ArrayList<>(slaveIds.size());
    for (String slaveId : slaveIds) {
      paths.add(getCurrentSlaveUsagePath(slaveId));
    }

    return getAsyncWithPath("getAllCurrentSlaveUsage", paths, slaveUsageTranscoder)
        .entrySet().stream()
        .map((entry) -> new SingularitySlaveUsageWithId(entry.getValue(), getSlaveIdFromCurrentUsagePath(entry.getKey())))
        .collect(Collectors.toList());
  }

  public List<SingularitySlaveUsageWithId> getAllCurrentSlaveUsage() {
    return getCurrentSlaveUsages(getSlavesWithUsage());
  }

  public List<Long> getSlaveUsageTimestamps(String slaveId) {
    List<String> timestampStrings = getChildren(getSlaveUsageHistoryPath(slaveId));
    List<Long> timestamps = new ArrayList<>(timestampStrings.size());
    for (String timestampString : timestampStrings) {
      timestamps.add(Long.parseLong(timestampString));
    }
    Collections.sort(timestamps);
    return timestamps;
  }

  public List<SingularityTaskCurrentUsageWithId> getTaskCurrentUsages(List<SingularityTaskId> taskIds) {
    List<String> paths = new ArrayList<>(taskIds.size());
    for (SingularityTaskId taskId : taskIds) {
      paths.add(getCurrentTaskUsagePath(taskId.getId()));
    }

    Map<String, SingularityTaskCurrentUsage> currentTaskUsages = getAsyncWithPath("getTaskCurrentUsages", paths, taskCurrentUsageTranscoder);
    List<SingularityTaskCurrentUsageWithId> currentTaskUsagesWithIds = new ArrayList<>(paths.size());
    for (Entry<String, SingularityTaskCurrentUsage> entry : currentTaskUsages.entrySet()) {
      currentTaskUsagesWithIds.add(new SingularityTaskCurrentUsageWithId(SingularityTaskId.valueOf(getTaskIdFromCurrentUsagePath(entry.getKey())), entry.getValue()));
    }

    return currentTaskUsagesWithIds;
  }

}
