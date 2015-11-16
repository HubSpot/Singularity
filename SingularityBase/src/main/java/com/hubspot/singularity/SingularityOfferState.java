package com.hubspot.singularity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class SingularityOfferState {
  private long timestamp;
  private List<SingularityTaskOfferResult> taskOfferResults;

  @JsonCreator
  public SingularityOfferState(@JsonProperty("timestamp") long timestamp,
                               @JsonProperty("taskOfferResults") List<SingularityTaskOfferResult> taskOfferResults) {
    this.timestamp = timestamp;
    this.taskOfferResults = taskOfferResults;
  }

  public static SingularityOfferState emptyState() {
    return new SingularityOfferState(System.currentTimeMillis(), new ArrayList<SingularityTaskOfferResult>());
  }

  public void addOfferResult(SingularityTaskOfferResult offerResult) {
    taskOfferResults.add(offerResult);
  }

  public SingularityTaskOfferResult getOfferResult(SingularityPendingTaskId pendingTaskId) {
    Optional<SingularityTaskOfferResult> maybeOfferResult = Optional.absent();
    for (SingularityTaskOfferResult offerResult : taskOfferResults) {
      if (pendingTaskId.getId().equals(offerResult.getTaskId().getId())) {
        maybeOfferResult = Optional.of(offerResult);
      }
    }
    if (maybeOfferResult.isPresent()) {
      return maybeOfferResult.get();
    } else {
      SingularityTaskOfferResult newOfferResult = new SingularityTaskOfferResult(pendingTaskId);
      taskOfferResults.add(newOfferResult);
      return newOfferResult;
    }
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public List<SingularityTaskOfferResult> getTaskOfferResults() {
    return taskOfferResults;
  }

  public void setTaskOfferResults(List<SingularityTaskOfferResult> taskOfferResults) {
    this.taskOfferResults = taskOfferResults;
  }

  public void trimOldData(int removeOlderThanMinutes) {
    long removeIfBefore = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(removeOlderThanMinutes);
    List<SingularityTaskOfferResult> toRemove = new ArrayList<>();
    for (SingularityTaskOfferResult offerResult : taskOfferResults) {
      if (offerResult.getTimestamp() < removeIfBefore) {
        toRemove.add(offerResult);
      }
    }
    taskOfferResults.removeAll(toRemove);
  }
}
