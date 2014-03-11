package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingularityRequest extends SingularityJsonObject {

  private final String id;
  
  private final List<String> owners;
  private final Integer numRetriesOnFailure;
  private final Integer maxFailuresBeforePausing;
  private final Boolean pauseOnInitialFailure;

  private final String schedule;
  private final Boolean daemon;
  
  private final Integer instances;
  private final Boolean rackSensitive;
  
  public static SingularityRequestBuilder newBuilder() {
    return new SingularityRequestBuilder();
  }

  public static SingularityRequest fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityRequest.class);
  }
  
  @JsonCreator
  public SingularityRequest(@JsonProperty("id") String id, @JsonProperty("owners") List<String> owners, @JsonProperty("numRetriesOnFailure") Integer numRetriesOnFailure,
      @JsonProperty("maxFailuresBeforePausing") Integer maxFailuresBeforePausing, @JsonProperty("pauseOnInitialFailure") Boolean pauseOnInitialFailure, 
      @JsonProperty("schedule") String schedule, @JsonProperty("daemon") Boolean daemon, @JsonProperty("instances") Integer instances, @JsonProperty("rackSensitive") Boolean rackSensitive) {
    this.id = id;
    this.owners = owners;
    this.numRetriesOnFailure = numRetriesOnFailure;
    this.maxFailuresBeforePausing = maxFailuresBeforePausing;
    this.pauseOnInitialFailure = pauseOnInitialFailure;
    this.schedule = schedule;
    this.daemon = daemon;
    this.rackSensitive = rackSensitive;
    this.instances = instances;
  }
  
  public SingularityRequestBuilder toBuilder() {
    return new SingularityRequestBuilder()
        .setDaemon(daemon)
        .setId(id)
        .setInstances(instances)
        .setMaxFailuresBeforePausing(maxFailuresBeforePausing)
        .setNumRetriesOnFailure(numRetriesOnFailure)
        .setOwners(owners == null ? null : Lists.newArrayList(owners))
        .setPauseOnInitialFailure(pauseOnInitialFailure)
        .setRackSensitive(rackSensitive)
        .setSchedule(schedule);
  }

  public Integer getInstances() {
    return instances;
  }

  public Boolean getRackSensitive() {
    return rackSensitive;
  }

  public String getId() {
    return id;
  }

  public List<String> getOwners() {
    return owners;
  }

  public Integer getNumRetriesOnFailure() {
    return numRetriesOnFailure;
  }

  public Integer getMaxFailuresBeforePausing() {
    return maxFailuresBeforePausing;
  }

  public Boolean getPauseOnInitialFailure() {
    return pauseOnInitialFailure;
  }

  public String getSchedule() {
    return schedule;
  }

  public Boolean getDaemon() {
    return daemon;
  }
  
  @JsonIgnore
  public boolean isScheduled() {
    return schedule != null;
  }
  
  @JsonIgnore
  public boolean isLongRunning() {
    return !isScheduled() && (daemon == null || daemon.booleanValue());
  }
  
  @JsonIgnore
  public boolean isOneOff() {
    return !isScheduled() && daemon != null && !daemon.booleanValue();
  }

  @JsonIgnore
  public boolean isRackSensitive() {
    return (rackSensitive != null && rackSensitive.booleanValue());
  }

  @Override
  public String toString() {
    return "SingularityRequest [id=" + id + ", owners=" + owners + ", numRetriesOnFailure=" + numRetriesOnFailure + ", maxFailuresBeforePausing=" + maxFailuresBeforePausing + ", pauseOnInitialFailure=" + pauseOnInitialFailure
        + ", schedule=" + schedule + ", daemon=" + daemon + ", instances=" + instances + ", rackSensitive=" + rackSensitive + "]";
  }
  
}
