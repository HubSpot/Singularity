package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingularityRequest extends SingularityJsonObject {

  private final String id;
  
  private final Optional<List<String>> owners;
  private final Optional<Integer> numRetriesOnFailure;
  private final Optional<Integer> maxFailuresBeforePausing;

  private final Optional<String> schedule;
  private final Optional<Boolean> daemon;
  
  private final Optional<Integer> instances;
  private final Optional<Boolean> rackSensitive;
  
  private final Optional<Boolean> loadBalanced;
  
  public static SingularityRequestBuilder newBuilder() {
    return new SingularityRequestBuilder();
  }

  public static SingularityRequest fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityRequest.class);
  }
  
  @JsonCreator
  public SingularityRequest(@JsonProperty("id") String id, @JsonProperty("owners") Optional<List<String>> owners, @JsonProperty("numRetriesOnFailure") Optional<Integer> numRetriesOnFailure,
      @JsonProperty("maxFailuresBeforePausing") Optional<Integer> maxFailuresBeforePausing, @JsonProperty("schedule") Optional<String> schedule, @JsonProperty("daemon") Optional<Boolean> daemon, 
      @JsonProperty("instances") Optional<Integer> instances, @JsonProperty("rackSensitive") Optional<Boolean> rackSensitive, @JsonProperty("loadBalanced") Optional<Boolean> loadBalanced) {
    this.id = id;
    this.owners = owners;
    this.numRetriesOnFailure = numRetriesOnFailure;
    this.maxFailuresBeforePausing = maxFailuresBeforePausing;
    this.schedule = schedule;
    this.daemon = daemon;
    this.rackSensitive = rackSensitive;
    this.instances = instances;
    this.loadBalanced = loadBalanced;
  }
  
  public SingularityRequestBuilder toBuilder() {
    return new SingularityRequestBuilder()
        .setDaemon(daemon)
        .setId(id)
        .setLoadBalanced(loadBalanced)
        .setInstances(instances)
        .setMaxFailuresBeforePausing(maxFailuresBeforePausing)
        .setNumRetriesOnFailure(numRetriesOnFailure)
        .setOwners(owners.isPresent() ? Optional.<List<String>> of(Lists.newArrayList(owners.get())) : owners)
        .setRackSensitive(rackSensitive)
        .setSchedule(schedule);
  }

  public String getId() {
    return id;
  }

  public Optional<List<String>> getOwners() {
    return owners;
  }

  public Optional<Integer> getNumRetriesOnFailure() {
    return numRetriesOnFailure;
  }

  public Optional<Integer> getMaxFailuresBeforePausing() {
    return maxFailuresBeforePausing;
  }

  public Optional<String> getSchedule() {
    return schedule;
  }

  public Optional<Boolean> getDaemon() {
    return daemon;
  }

  public Optional<Integer> getInstances() {
    return instances;
  }

  public Optional<Boolean> getRackSensitive() {
    return rackSensitive;
  }
  
  public Optional<Boolean> getLoadBalanced() {
    return loadBalanced;
  }

  @JsonIgnore
  public int getInstancesSafe() {
    return getInstances().or(1);
  }
  
  @JsonIgnore
  public boolean isScheduled() {
    return schedule.isPresent();
  }
  
  @JsonIgnore
  public boolean isDaemon() {
    return daemon.or(Boolean.TRUE).booleanValue();
  }
  
  @JsonIgnore
  public boolean isLongRunning() {
    return !isScheduled() && isDaemon();
  }
  
  @JsonIgnore
  public boolean isOneOff() {
    return !isScheduled() && !isDaemon();
  }
  
  @JsonIgnore
  public boolean isDeployable() {
    return !isScheduled() && !isOneOff();
  }
  
  @JsonIgnore
  public boolean isRackSensitive() {
    return rackSensitive.or(Boolean.FALSE).booleanValue();
  }
  
  @JsonIgnore
  public boolean isLoadBalanced() {
    return loadBalanced.or(Boolean.FALSE).booleanValue();
  }

  @Override
  public String toString() {
    return "SingularityRequest [id=" + id + ", owners=" + owners + ", numRetriesOnFailure=" + numRetriesOnFailure + ", maxFailuresBeforePausing=" + maxFailuresBeforePausing + ", schedule=" + schedule + ", daemon=" + daemon + ", instances="
        + instances + ", rackSensitive=" + rackSensitive + ", loadBalanced=" + loadBalanced + "]";
  }
  
}
