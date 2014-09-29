package com.hubspot.singularity;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityRequest extends SingularityJsonObject {

  private final String id;

  private final Optional<List<String>> owners;
  private final Optional<Integer> numRetriesOnFailure;

  private final Optional<String> schedule;
  private final Optional<String> quartzSchedule;
  private final Optional<ScheduleType> scheduleType;

  private final Optional<Long> killOldNonLongRunningTasksAfterMillis;

  private final Optional<Boolean> daemon;

  private final Optional<Integer> instances;

  private final Optional<Boolean> rackSensitive;
  private final Optional<List<String>> rackAffinity;
  private final Optional<SlavePlacement> slavePlacement;

  private final Optional<Boolean> loadBalanced;

  public static SingularityRequestBuilder newBuilder(String id) {
    return new SingularityRequestBuilder(id);
  }

  public static SingularityRequest fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityRequest.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  @JsonCreator
  public SingularityRequest(@JsonProperty("id") String id, @JsonProperty("owners") Optional<List<String>> owners, @JsonProperty("numRetriesOnFailure") Optional<Integer> numRetriesOnFailure,
      @JsonProperty("schedule") Optional<String> schedule, @JsonProperty("daemon") Optional<Boolean> daemon, @JsonProperty("instances") Optional<Integer> instances,
      @JsonProperty("rackSensitive") Optional<Boolean> rackSensitive, @JsonProperty("loadBalanced") Optional<Boolean> loadBalanced,
      @JsonProperty("killOldNonLongRunningTasksAfterMillis") Optional<Long> killOldNonLongRunningTasksAfterMillis, @JsonProperty("scheduleType") Optional<ScheduleType> scheduleType,
      @JsonProperty("quartzSchedule") Optional<String> quartzSchedule, @JsonProperty("rackAffinity") Optional<List<String>> rackAffinity,
      @JsonProperty("slavePlacement") Optional<SlavePlacement> slavePlacement) {
    this.id = id;
    this.owners = owners;
    this.numRetriesOnFailure = numRetriesOnFailure;
    this.schedule = schedule;
    this.daemon = daemon;
    this.rackSensitive = rackSensitive;
    this.instances = instances;
    this.loadBalanced = loadBalanced;
    this.killOldNonLongRunningTasksAfterMillis = killOldNonLongRunningTasksAfterMillis;
    this.scheduleType = scheduleType;
    this.quartzSchedule = quartzSchedule;
    this.rackAffinity = rackAffinity;
    this.slavePlacement = slavePlacement;
  }

  public SingularityRequestBuilder toBuilder() {
    return new SingularityRequestBuilder(id)
    .setDaemon(daemon)
    .setLoadBalanced(loadBalanced)
    .setInstances(instances)
    .setNumRetriesOnFailure(numRetriesOnFailure)
    .setOwners(copyOfList(owners))
    .setRackSensitive(rackSensitive)
    .setSchedule(schedule)
    .setKillOldNonLongRunningTasksAfterMillis(killOldNonLongRunningTasksAfterMillis)
    .setScheduleType(scheduleType)
    .setRackAffinity(copyOfList(rackAffinity))
    .setSlavePlacement(slavePlacement);
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

  public Optional<String> getQuartzSchedule() {
    return quartzSchedule;
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

  public Optional<Long> getKillOldNonLongRunningTasksAfterMillis() {
    return killOldNonLongRunningTasksAfterMillis;
  }

  public Optional<ScheduleType> getScheduleType() {
    return scheduleType;
  }

  public Optional<List<String>> getRackAffinity() {
    return rackAffinity;
  }

  public Optional<SlavePlacement> getSlavePlacement() {
    return slavePlacement;
  }

  @JsonIgnore
  public int getInstancesSafe() {
    return getInstances().orElse(1);
  }

  @JsonIgnore
  public boolean isScheduled() {
    return schedule.isPresent() || quartzSchedule.isPresent();
  }

  @JsonIgnore
  public String getQuartzScheduleSafe() {
    if (quartzSchedule.isPresent()) {
      return quartzSchedule.get();
    }

    return schedule.get();
  }

  @JsonIgnore
  public boolean isDaemon() {
    return daemon.orElse(Boolean.TRUE).booleanValue();
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
    return rackSensitive.orElse(Boolean.FALSE).booleanValue();
  }

  @JsonIgnore
  public boolean isLoadBalanced() {
    return loadBalanced.orElse(Boolean.FALSE).booleanValue();
  }

  @JsonIgnore
  public ScheduleType getScheduleTypeSafe() {
    return scheduleType.orElse(ScheduleType.CRON);
  }

  @Override
  public String toString() {
    return "SingularityRequest [id=" + id + ", owners=" + owners + ", numRetriesOnFailure=" + numRetriesOnFailure + ", schedule=" + schedule + ", quartzSchedule=" + quartzSchedule + ", scheduleType="
        + scheduleType + ", killOldNonLongRunningTasksAfterMillis=" + killOldNonLongRunningTasksAfterMillis + ", daemon=" + daemon + ", instances=" + instances + ", rackSensitive=" + rackSensitive
        + ", rackAffinity=" + rackAffinity + ", slavePlacement=" + slavePlacement + ", loadBalanced=" + loadBalanced + "]";
  }

}
