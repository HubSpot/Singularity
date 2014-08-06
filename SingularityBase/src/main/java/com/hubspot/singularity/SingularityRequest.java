package com.hubspot.singularity;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class SingularityRequest extends SingularityJsonObject {

  private final String id;
  
  private final Optional<List<String>> owners;
  private final Optional<Integer> numRetriesOnFailure;

  private final Optional<String> schedule;
  private final Optional<Boolean> daemon;
  
  private final Optional<Integer> instances;
  private final Optional<Boolean> rackSensitive;
  
  private final Optional<Boolean> loadBalanced;
  
  public static SingularityRequestBuilder newBuilder(final String id) {
    return new SingularityRequestBuilder(id);
  }

  public static SingularityRequest fromBytes(final byte[] bytes, final ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityRequest.class);
    } catch (final IOException e) {
      throw new SingularityJsonException(e);
    }
  }
  
  @JsonCreator
  public SingularityRequest(@JsonProperty("id") final String id, @JsonProperty("owners") final Optional<List<String>> owners, @JsonProperty("numRetriesOnFailure") final Optional<Integer> numRetriesOnFailure,
      @JsonProperty("schedule") final Optional<String> schedule, @JsonProperty("daemon") final Optional<Boolean> daemon,  @JsonProperty("instances") final Optional<Integer> instances, 
      @JsonProperty("rackSensitive") final Optional<Boolean> rackSensitive, @JsonProperty("loadBalanced") final Optional<Boolean> loadBalanced) {
    this.id = id;
    this.owners = owners;
    this.numRetriesOnFailure = numRetriesOnFailure;
    this.schedule = schedule;
    this.daemon = daemon;
    this.rackSensitive = rackSensitive;
    this.instances = instances;
    this.loadBalanced = loadBalanced;
  }
  
  public SingularityRequestBuilder toBuilder() {
    return new SingularityRequestBuilder(id)
        .setDaemon(daemon)
        .setLoadBalanced(loadBalanced)
        .setInstances(instances)
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
    return "SingularityRequest [id=" + id + ", owners=" + owners + ", numRetriesOnFailure=" + numRetriesOnFailure + ", schedule=" + schedule + ", daemon=" + daemon + ", instances=" + instances + ", rackSensitive=" + rackSensitive
        + ", loadBalanced=" + loadBalanced + "]";
  }  
  
}
