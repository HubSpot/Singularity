package com.hubspot.singularity;

import java.util.List;

import com.google.common.base.Optional;

public class SingularityRequestBuilder {

  private String id;
  
  private Optional<List<String>> owners;
  private Optional<Integer> numRetriesOnFailure;
  private Optional<Integer> maxFailuresBeforePausing;

  private Optional<String> schedule;
  private Optional<Boolean> daemon;
  
  private Optional<Integer> instances;
  private Optional<Boolean> rackSensitive;
  
  public SingularityRequest build() {
    return new SingularityRequest(id, owners, numRetriesOnFailure, maxFailuresBeforePausing, schedule, daemon, instances, rackSensitive);
  }

  public String getId() {
    return id;
  }

  public SingularityRequestBuilder setId(String id) {
    this.id = id;
    return this;
  }

  public Optional<List<String>> getOwners() {
    return owners;
  }

  public SingularityRequestBuilder setOwners(Optional<List<String>> owners) {
    this.owners = owners;
    return this;
  }

  public Optional<Integer> getNumRetriesOnFailure() {
    return numRetriesOnFailure;
  }

  public SingularityRequestBuilder setNumRetriesOnFailure(Optional<Integer> numRetriesOnFailure) {
    this.numRetriesOnFailure = numRetriesOnFailure;
    return this;
  }

  public Optional<Integer> getMaxFailuresBeforePausing() {
    return maxFailuresBeforePausing;
  }

  public SingularityRequestBuilder setMaxFailuresBeforePausing(Optional<Integer> maxFailuresBeforePausing) {
    this.maxFailuresBeforePausing = maxFailuresBeforePausing;
    return this;
  }

  public Optional<String> getSchedule() {
    return schedule;
  }

  public SingularityRequestBuilder setSchedule(Optional<String> schedule) {
    this.schedule = schedule;
    return this;
  }

  public Optional<Boolean> getDaemon() {
    return daemon;
  }

  public SingularityRequestBuilder setDaemon(Optional<Boolean> daemon) {
    this.daemon = daemon;
    return this;
  }

  public Optional<Integer> getInstances() {
    return instances;
  }

  public SingularityRequestBuilder setInstances(Optional<Integer> instances) {
    this.instances = instances;
    return this;
  }

  public Optional<Boolean> getRackSensitive() {
    return rackSensitive;
  }

  public SingularityRequestBuilder setRackSensitive(Optional<Boolean> rackSensitive) {
    this.rackSensitive = rackSensitive;
    return this;
  }

  @Override
  public String toString() {
    return "SingularityRequestBuilder [id=" + id + ", owners=" + owners + ", numRetriesOnFailure=" + numRetriesOnFailure + ", maxFailuresBeforePausing=" + maxFailuresBeforePausing
        + ", schedule=" + schedule + ", daemon=" + daemon + ", instances=" + instances + ", rackSensitive=" + rackSensitive + "]";
  }
  
}
