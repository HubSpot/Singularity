package com.hubspot.singularity;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class SingularityTaskOfferResult {
  private final long timestamp;
  private final SingularityPendingTaskId taskId;
  private Map<String, String> offerDeclinedReasons = new HashMap<>();
  private Optional<String> launchedOnHost = Optional.absent();
  private Optional<String> acceptedOfferId = Optional.absent();

  public SingularityTaskOfferResult(SingularityPendingTaskId taskId) {
    this.taskId = taskId;
    this.timestamp = System.currentTimeMillis();
  }

  @JsonCreator
  public SingularityTaskOfferResult(@JsonProperty("timestamp") long timestamp,
                                    @JsonProperty("taskId") SingularityPendingTaskId taskId,
                                    @JsonProperty("offerDeclinedReasons") Map<String, String> offerDeclinedReasons,
                                    @JsonProperty("launchedOnHost") Optional<String> launchedOnHost) {
    this.timestamp = timestamp;
    this.taskId = taskId;
    this.offerDeclinedReasons = offerDeclinedReasons;
    this.launchedOnHost = launchedOnHost;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public SingularityPendingTaskId getTaskId() {
    return taskId;
  }

  public Map<String, String> getOfferDeclinedReasons() {
    return offerDeclinedReasons;
  }

  public void setOfferDeclinedReasons(Map<String, String> offerDeclinedReasons) {
    this.offerDeclinedReasons = offerDeclinedReasons;
  }

  public Optional<String> getLaunchedOnHost() {
    return launchedOnHost;
  }

  public void setLaunchedOnHost(Optional<String> launchedOnHost) {
    this.launchedOnHost = launchedOnHost;
  }

  public Optional<String> getAcceptedOfferId() {
    return acceptedOfferId;
  }

  public void setAcceptedOfferId(Optional<String> acceptedOfferId) {
    this.acceptedOfferId = acceptedOfferId;
  }

  public void setLaunchedTaskInfo(Optional<String> launchedOnHost, Optional<String> acceptedOfferId) {
    offerDeclinedReasons.clear();
    this.launchedOnHost = launchedOnHost;
    this.acceptedOfferId = acceptedOfferId;
  }

  public void addOfferDeclinedReason(String host, String reason) {
    offerDeclinedReasons.put(host, reason);
  }
}
