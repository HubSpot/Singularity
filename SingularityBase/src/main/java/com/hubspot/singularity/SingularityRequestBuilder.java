package com.hubspot.singularity;

import java.util.List;
import java.util.Optional;

public class SingularityRequestBuilder {

  private final String id;

  private Optional<List<String>> owners;
  private Optional<Integer> numRetriesOnFailure;

  private Optional<String> schedule;
  private Optional<String> quartzSchedule;
  private Optional<ScheduleType> scheduleType;

  private Optional<Long> killOldNonLongRunningTasksAfterMillis;

  private Optional<Boolean> daemon;

  private Optional<Integer> instances;

  private Optional<Boolean> rackSensitive;
  private Optional<List<String>> rackAffinity;
  private Optional<SlavePlacement> slavePlacement;

  private Optional<Boolean> loadBalanced;

  public SingularityRequestBuilder(String id) {
    this.id = id;
    this.owners = Optional.empty();
    this.numRetriesOnFailure = Optional.empty();
    this.schedule = Optional.empty();
    this.scheduleType = Optional.empty();
    this.killOldNonLongRunningTasksAfterMillis = Optional.empty();
    this.daemon = Optional.empty();
    this.instances = Optional.empty();
    this.rackSensitive = Optional.empty();
    this.loadBalanced = Optional.empty();
    this.quartzSchedule = Optional.empty();
    this.rackAffinity = Optional.empty();
    this.slavePlacement = Optional.empty();
  }

  public SingularityRequest build() {
    return new SingularityRequest(id, owners, numRetriesOnFailure, schedule, daemon, instances, rackSensitive, loadBalanced, killOldNonLongRunningTasksAfterMillis, scheduleType, quartzSchedule, rackAffinity, slavePlacement);
  }

  public Optional<Boolean> getLoadBalanced() {
    return loadBalanced;
  }

  public SingularityRequestBuilder setLoadBalanced(Optional<Boolean> loadBalanced) {
    this.loadBalanced = loadBalanced;
    return this;
  }

  public String getId() {
    return id;
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

  public Optional<Long> getKillOldNonLongRunningTasksAfterMillis() {
    return killOldNonLongRunningTasksAfterMillis;
  }

  public SingularityRequestBuilder setKillOldNonLongRunningTasksAfterMillis(Optional<Long> killOldNonLongRunningTasksAfterMillis) {
    this.killOldNonLongRunningTasksAfterMillis = killOldNonLongRunningTasksAfterMillis;
    return this;
  }

  public Optional<ScheduleType> getScheduleType() {
    return scheduleType;
  }

  public SingularityRequestBuilder setScheduleType(Optional<ScheduleType> scheduleType) {
    this.scheduleType = scheduleType;
    return this;
  }

  public Optional<String> getQuartzSchedule() {
    return quartzSchedule;
  }

  public SingularityRequestBuilder setQuartzSchedule(Optional<String> quartzSchedule) {
    this.quartzSchedule = quartzSchedule;
    return this;
  }

  public Optional<List<String>> getRackAffinity() {
    return rackAffinity;
  }

  public SingularityRequestBuilder setRackAffinity(Optional<List<String>> rackAffinity) {
    this.rackAffinity = rackAffinity;
    return this;
  }

  public Optional<SlavePlacement> getSlavePlacement() {
    return slavePlacement;
  }

  public SingularityRequestBuilder setSlavePlacement(Optional<SlavePlacement> slavePlacement) {
    this.slavePlacement = slavePlacement;
    return this;
  }

  @Override
  public String toString() {
    return "SingularityRequestBuilder [id=" + id + ", owners=" + owners + ", numRetriesOnFailure=" + numRetriesOnFailure + ", schedule=" + schedule + ", quartzSchedule=" + quartzSchedule
        + ", scheduleType=" + scheduleType + ", killOldNonLongRunningTasksAfterMillis=" + killOldNonLongRunningTasksAfterMillis + ", daemon=" + daemon + ", instances=" + instances
        + ", rackSensitive=" + rackSensitive + ", rackAffinity=" + rackAffinity + ", slavePlacement=" + slavePlacement + ", loadBalanced=" + loadBalanced + "]";
  }

}
