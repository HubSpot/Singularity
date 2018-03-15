package com.hubspot.singularity.api.task;

import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.api.common.SingularityHistoryItem;
import com.hubspot.singularity.api.common.SingularityId;
import com.hubspot.singularity.exceptions.InvalidSingularityTaskIdException;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The unique id for a singularity task")
public class SingularityTaskId extends SingularityId implements SingularityHistoryItem {

  private final String id;
  private final String requestId;
  private final String deployId;
  private final long startedAt;
  private final int instanceNo;
  private final String sanitizedHost;
  private final String sanitizedRackId;

  public static Comparator<SingularityTaskId> INSTANCE_NO_COMPARATOR = (o1, o2) -> Integer.compare(o1.instanceNo, o2.instanceNo);

  public static Comparator<SingularityTaskId> STARTED_AT_COMPARATOR_DESC = (o1, o2) -> Long.compare(o2.startedAt, o1.startedAt);

  public SingularityTaskId(String requestId, String deployId, long startedAt, int instanceNo, String sanitizedHost, String sanitizedRackId) {
    this.id = String.format("%s-%s-%s-%s-%s-%s", requestId, deployId, startedAt, instanceNo, sanitizedHost, sanitizedRackId);
    this.requestId = requestId;
    this.deployId = deployId;
    this.startedAt = startedAt;
    this.instanceNo = instanceNo;
    this.sanitizedHost = sanitizedHost;
    this.sanitizedRackId = sanitizedRackId;
  }

  @JsonCreator
  public SingularityTaskId(@JsonProperty("requestId") String requestId,
                           @JsonProperty("deployId") String deployId,
                           @JsonProperty("nextRunAt") Long nextRunAt,
                           @JsonProperty("startedAt") Long startedAt,
                           @JsonProperty("instanceNo") int instanceNo,
                           @JsonProperty("host") String host,
                           @JsonProperty("sanitizedHost") String sanitizedHost,
                           @JsonProperty("sanitizedRackId") String sanitizedRackId,
                           @JsonProperty("rackId") String rackId) {
    this(requestId, deployId, startedAt != null ? startedAt : nextRunAt, instanceNo, sanitizedHost != null ? sanitizedHost : host, sanitizedRackId != null ? sanitizedRackId : rackId);
  }

  public String getId() {
    return id;
  }

  /**
   * @Deprecated use getSanitizedRackId() or matchesOriginalRackId() instead
   */
  @Deprecated
  @Schema(description = "The id of the rack where this task was launched")
  public String getRackId() {
    return getSanitizedRackId();
  }

  @Schema(
      title = "The id of the rack where this task was launched",
      description = "- characters are repalced with _ in this id due to the fact that sections of the task id are delimited by -'s"
  )
  public String getSanitizedRackId() {
    return sanitizedRackId;
  }

  @JsonIgnore
  public boolean matchesOriginalRackId(String unsanitizedRackId) {
    return sanitizedRackId.equals(JavaUtils.getReplaceHyphensWithUnderscores(unsanitizedRackId));
  }

  @Schema(description = "The deploy associated with this task")
  public String getDeployId() {
    return deployId;
  }

  /**
   * @Deprecated use getSanitizedHost() or matchesOriginalHost() instead
   */
  @Deprecated
  @Schema(description = "The hostname of the machine where this task was launched")
  public String getHost() {
    return getSanitizedHost();
  }

  @Schema(
      title = "The hostname of the machine where this task was launched",
      description = "- characters are repalced with _ in this id due to the fact that sections of the task id are delimited by -'s"
  )
  public String getSanitizedHost() {
    return sanitizedHost;
  }

  @JsonIgnore
  public boolean matchesOriginalHost(String unsanitizedHost) {
    return sanitizedHost.equals(JavaUtils.getReplaceHyphensWithUnderscores(unsanitizedHost));
  }

  @Schema(description = "The request associated with this task")
  public String getRequestId() {
    return requestId;
  }

  @Schema(description = "The time at which this task was started")
  public long getStartedAt() {
    return startedAt;
  }

  @Schema(description = "The instance number for this task", minimum = "1")
  public int getInstanceNo() {
    return instanceNo;
  }

  @Override
  @JsonIgnore
  public long getCreateTimestampForCalculatingHistoryAge() {
    return getStartedAt();
  }

  public static SingularityTaskId valueOf(String string) throws InvalidSingularityTaskIdException {
    String[] splits = null;

    try {
      splits = JavaUtils.reverseSplit(string, 6, "-");
    } catch (IllegalStateException ise) {
      throw new InvalidSingularityTaskIdException(String.format("TaskId %s was invalid (%s)", string, ise.getMessage()));
    }

    try {
      final String requestId = splits[0];
      final String deployId = splits[1];
      final long startedAt = Long.parseLong(splits[2]);
      final int instanceNo = Integer.parseInt(splits[3]);
      final String host = splits[4];
      final String rackId = splits[5];

      return new SingularityTaskId(requestId, deployId, startedAt, instanceNo, host, rackId);
    } catch (IllegalArgumentException e) {
      throw new InvalidSingularityTaskIdException(String.format("TaskId %s had an invalid parameter (%s)", string, e.getMessage()));
    }
  }

  @Override
  public String toString() {
    return getId();
  }
}
