package com.hubspot.singularity.data.usage;

import java.util.ArrayList;
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
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.CuratorAsyncManager;
import com.hubspot.singularity.data.SingularityWebCache;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;

@Singleton
public class UsageManager extends CuratorAsyncManager {

  private static final String ROOT_PATH = "/usage";

  private static final String SLAVE_PATH = ROOT_PATH + "/slaves";
  private static final String REQUESTS_PATH = ROOT_PATH + "/requests";
  private static final String USAGE_SUMMARY_PATH = ROOT_PATH + "/summary";

  private static final String CURRENT_USAGE_NODE_KEY = "CURRENT";

  private final Transcoder<SingularitySlaveUsage> slaveUsageTranscoder;
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
                      Transcoder<SingularitySlaveUsage> slaveUsageTranscoder,
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

  public List<String> getSlavesWithUsage() {
    return getChildren(SLAVE_PATH);
  }

  // /slaves/<slaveid>
  private String getSlaveIdFromCurrentUsagePath(String path) {
    return path.substring(path.indexOf(SLAVE_PATH) + SLAVE_PATH.length() + 1, path.lastIndexOf("/"));
  }

  private String getSlaveUsagePath(String slaveId) {
    return ZKPaths.makePath(SLAVE_PATH, slaveId);
  }

  private String getCurrentSlaveUsagePath(String slaveId) {
    return ZKPaths.makePath(getSlaveUsagePath(slaveId), CURRENT_USAGE_NODE_KEY);
  }

  private String getRequestPath(String requestId) {
    return ZKPaths.makePath(REQUESTS_PATH, requestId);
  }

  public SingularityDeleteResult deleteSlaveUsage(String slaveId) {
    return delete(getSlaveUsagePath(slaveId));
  }

  public SingularityCreateResult saveClusterUtilization(SingularityClusterUtilization utilization) {
    return save(USAGE_SUMMARY_PATH, utilization, clusterUtilizationTranscoder);
  }

  public SingularityCreateResult saveCurrentSlaveUsage(String slaveId, SingularitySlaveUsage usage) {
    return set(getCurrentSlaveUsagePath(slaveId), usage, slaveUsageTranscoder);
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
}
