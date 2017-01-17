package com.hubspot.singularity.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class UsageManager extends CuratorAsyncManager {

  private static final String ROOT_PATH = "/usage";

  private static final String SLAVE_PATH = ROOT_PATH + "/slaves";
  private static final String TASK_PATH = ROOT_PATH + "/tasks";

  private final Transcoder<SingularitySlaveUsage> slaveUsageTranscoder;
  private final Transcoder<SingularityTaskUsage> taskUsageTranscoder;

  @Inject
  public UsageManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry, Transcoder<SingularitySlaveUsage> slaveUsageTranscoder,
      Transcoder<SingularityTaskUsage> taskUsageTranscoder) {
    super(curator, configuration, metricRegistry);

    this.slaveUsageTranscoder = slaveUsageTranscoder;
    this.taskUsageTranscoder = taskUsageTranscoder;
  }

  public List<String> getSlavesWithUsage() {
    return getChildren(SLAVE_PATH);
  }

  public List<String> getTasksWithUsage() {
    return getChildren(TASK_PATH);
  }

  private String getSlaveUsagePath(String slaveId) {
    return ZKPaths.makePath(SLAVE_PATH, slaveId);
  }

  private String getTaskUsagePath(String taskId) {
    return ZKPaths.makePath(TASK_PATH, taskId);
  }

  private String getSpecificSlaveUsagePath(String slaveId, long timestamp) {
    return ZKPaths.makePath(getSlaveUsagePath(slaveId), Long.toString(timestamp));
  }

  private String getSpecificTaskUsagePath(String taskId, double timestamp) {
    return ZKPaths.makePath(getTaskUsagePath(taskId), Double.toString(timestamp));
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

  public SingularityCreateResult saveSpecificTaskUsage(String taskId, SingularityTaskUsage usage) {
    return save(getSpecificTaskUsagePath(taskId, usage.getTimestamp()), usage, taskUsageTranscoder);
  }

  public SingularityCreateResult saveSpecificSlaveUsage(String slaveId, SingularitySlaveUsage usage) {
    return save(getSpecificSlaveUsagePath(slaveId, usage.getTimestamp()), usage, slaveUsageTranscoder);
  }

  private static final Comparator<SingularitySlaveUsage> SLAVE_USAGE_COMPARATOR_TIMESTAMP_ASC = new Comparator<SingularitySlaveUsage>() {

    @Override
    public int compare(SingularitySlaveUsage o1, SingularitySlaveUsage o2) {
      return Long.compare(o1.getTimestamp(), o2.getTimestamp());
    }

  };

  public List<SingularitySlaveUsage> getSlaveUsage(String slaveId) {
    List<SingularitySlaveUsage> children = getAsyncChildren(getSlaveUsagePath(slaveId), slaveUsageTranscoder);
    Collections.sort(children, SLAVE_USAGE_COMPARATOR_TIMESTAMP_ASC);
    return children;
  }

  private static final Comparator<SingularityTaskUsage> TASK_USAGE_COMPARATOR_TIMESTAMP_ASC = new Comparator<SingularityTaskUsage>() {

    @Override
    public int compare(SingularityTaskUsage o1, SingularityTaskUsage o2) {
      return Double.compare(o1.getTimestamp(), o2.getTimestamp());
    }

  };

  public List<SingularityTaskUsage> getTaskUsage(String taskId) {
    List<SingularityTaskUsage> children = getAsyncChildren(getTaskUsagePath(taskId), taskUsageTranscoder);
    Collections.sort(children, TASK_USAGE_COMPARATOR_TIMESTAMP_ASC);
    return children;
  }

  public List<Long> getSlaveUsageTimestamps(String slaveId) {
    List<String> timestampStrings = getChildren(getSlaveUsagePath(slaveId));
    List<Long> timestamps = new ArrayList<>(timestampStrings.size());
    for (String timestampString : timestampStrings) {
      timestamps.add(Long.parseLong(timestampString));
    }
    Collections.sort(timestamps);
    return timestamps;
  }

}
