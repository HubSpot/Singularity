package com.hubspot.singularity;

import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.JavaUtils;

public class SingularityTaskId extends SingularityId implements SingularityHistoryItem {

  private final String requestId;
  private final String deployId;
  private final long startedAt;
  private final int instanceNo;
  private final String sanitizedHost;
  private final String sanitizedRackId;

  public static Comparator<SingularityTaskId> INSTANCE_NO_COMPARATOR = new Comparator<SingularityTaskId>() {

    @Override
    public int compare(SingularityTaskId o1, SingularityTaskId o2) {
      return Integer.compare(o1.instanceNo, o2.instanceNo);
    }

  };

  public static Comparator<SingularityTaskId> STARTED_AT_COMPARATOR_DESC = new Comparator<SingularityTaskId>() {

    @Override
    public int compare(SingularityTaskId o1, SingularityTaskId o2) {
      return Long.compare(o2.startedAt, o1.startedAt);
    }

  };

  public SingularityTaskId(String requestId, String deployId, long startedAt, int instanceNo, String sanitizedHost, String sanitizedRackId) {
    super(String.format("%s-%s-%s-%s-%s-%s", requestId, deployId, startedAt, instanceNo, sanitizedHost, sanitizedRackId));
    this.requestId = requestId;
    this.deployId = deployId;
    this.startedAt = startedAt;
    this.instanceNo = instanceNo;
    this.sanitizedHost = sanitizedHost;
    this.sanitizedRackId = sanitizedRackId;
  }

  @JsonCreator
  public SingularityTaskId(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("nextRunAt") Long nextRunAt, @JsonProperty("startedAt") Long startedAt,
      @JsonProperty("instanceNo") int instanceNo, @JsonProperty("host") String host, @JsonProperty("sanitizedHost") String sanitizedHost,
      @JsonProperty("sanitizedRackId") String sanitizedRackId, @JsonProperty("rackId") String rackId) {
    this(requestId, deployId, startedAt != null ? startedAt : nextRunAt, instanceNo, sanitizedHost != null ? sanitizedHost : host, sanitizedRackId != null ? sanitizedRackId : rackId);
  }

  /**
   * @Deprecated use getSanitizedRackId() or matchesOriginalRackId() instead
   */
  public String getRackId() {
    return getSanitizedRackId();
  }

  public String getSanitizedRackId() {
    return sanitizedRackId;
  }

  @JsonIgnore
  public boolean matchesOriginalRackId(String unsanitizedRackId) {
    return sanitizedRackId.equals(JavaUtils.getReplaceHyphensWithUnderscores(unsanitizedRackId));
  }

  public String getDeployId() {
    return deployId;
  }

  /**
   * @Deprecated use getSanitizedHost() or matchesOriginalHost() instead
   */
  public String getHost() {
    return getSanitizedHost();
  }

  public String getSanitizedHost() {
    return sanitizedHost;
  }

  @JsonIgnore
  public boolean matchesOriginalHost(String unsanitizedHost) {
    return sanitizedHost.equals(JavaUtils.getReplaceHyphensWithUnderscores(unsanitizedHost));
  }

  public String getRequestId() {
    return requestId;
  }

  public long getStartedAt() {
    return startedAt;
  }

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
