package com.hubspot.singularity;

import java.util.List;

public class SingularityRequestBuilder {

  private String id;

  private String schedule;
  private Boolean daemon;
  
  private List<String> owners;
  private Integer numRetriesOnFailure;
  private Integer maxFailuresBeforePausing;
  private Boolean pauseOnInitialFailure;
 
  private Integer instances;
  private Boolean rackSensitive;
  
  public SingularityRequest build() {
    return new SingularityRequest(id, owners, numRetriesOnFailure, maxFailuresBeforePausing, pauseOnInitialFailure, schedule, daemon, instances, rackSensitive);
  }

  public SingularityRequestBuilder setSchedule(String schedule) {
    this.schedule = schedule;
    return this;
  }

  public Integer getInstances() {
    return instances;
  }

  public SingularityRequestBuilder setInstances(Integer instances) {
    this.instances = instances;
    return this;
  }

  public Boolean getRackSensitive() {
    return rackSensitive;
  }

  public SingularityRequestBuilder setRackSensitive(Boolean rackSensitive) {
    this.rackSensitive = rackSensitive;
    return this;
  }

  public String getSchedule() {
    return schedule;
  }

  public Boolean getDaemon() {
    return daemon;
  }

  public SingularityRequestBuilder setDaemon(Boolean daemon) {
    this.daemon = daemon;
    return this;
  }

  public Integer getMaxFailuresBeforePausing() {
    return maxFailuresBeforePausing;
  }

  public SingularityRequestBuilder setMaxFailuresBeforePausing(Integer maxFailuresBeforePausing) {
    this.maxFailuresBeforePausing = maxFailuresBeforePausing;
    return this;
  }

  public List<String> getOwners() {
    return owners;
  }

  public SingularityRequestBuilder setOwners(List<String> owners) {
    this.owners = owners;
    return this;
  }

  public Integer getNumRetriesOnFailure() {
    return numRetriesOnFailure;
  }

  public SingularityRequestBuilder setNumRetriesOnFailure(Integer numRetriesOnFailure) {
    this.numRetriesOnFailure = numRetriesOnFailure;
    return this;
  }

  public String getId() {
    return id;
  }

  public SingularityRequestBuilder setId(String id) {
    this.id = id;
    return this;
  }

  public Boolean getPauseOnInitialFailure() {
    return pauseOnInitialFailure;
  }

  public SingularityRequestBuilder setPauseOnInitialFailure(Boolean pauseOnInitialFailure) {
    this.pauseOnInitialFailure = pauseOnInitialFailure;
    return this;
  }

  @Override
  public String toString() {
    return "SingularityRequestBuilder [id=" + id + ", schedule=" + schedule + ", daemon=" + daemon + ", owners=" + owners + ", numRetriesOnFailure=" + numRetriesOnFailure + ", maxFailuresBeforePausing=" + maxFailuresBeforePausing
        + ", pauseOnInitialFailure=" + pauseOnInitialFailure + ", instances=" + instances + ", rackSensitive=" + rackSensitive + "]";
  }
  
}
