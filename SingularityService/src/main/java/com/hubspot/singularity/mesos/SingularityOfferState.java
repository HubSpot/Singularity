package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.SingularityTaskRequest;

@JsonIgnoreProperties( ignoreUnknown = true )
public class SingularityOfferState {
  private long timestamp;
  private List<SingularityOfferHolder> offerHolders;
  private List<SingularityTaskRequest> remainingTasks;

  @JsonCreator
  public SingularityOfferState(@JsonProperty("timestamp") long timestamp,
                               @JsonProperty("offerHolders") List<SingularityOfferHolder> offerHolders,
                               @JsonProperty("remainingTasks") List<SingularityTaskRequest> remainingTasks) {
    this.timestamp = timestamp;
    this.offerHolders = offerHolders;
    this.remainingTasks = remainingTasks;
  }

  public static SingularityOfferState emptyState() {
    return new SingularityOfferState(System.currentTimeMillis(), Collections.<SingularityOfferHolder>emptyList(), Collections.<SingularityTaskRequest>emptyList());
  }

  public long getTimestamp() {
    return timestamp;
  }

  public List<SingularityOfferHolder> getOfferHolders() {
    return offerHolders;
  }

  public List<SingularityTaskRequest> getRemainingTasks() {
    return remainingTasks;
  }
}
