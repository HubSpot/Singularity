package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import static com.hubspot.singularity.JsonHelpers.copyOfList;

public class SingularityRequest {

  private final String id;
  private final RequestType requestType;

  private final Optional<List<String>> owners;
  private final Optional<Integer> numRetriesOnFailure;

  private final Optional<String> schedule;
  private final Optional<String> quartzSchedule;
  private final Optional<ScheduleType> scheduleType;

  private final Optional<Long> killOldNonLongRunningTasksAfterMillis;
  private final Optional<Long> scheduledExpectedRuntimeMillis;

  private final Optional<Long> waitAtLeastMillisAfterTaskFinishesForReschedule;

  //"use requestType instead"
  @Deprecated
  private final Optional<Boolean> daemon;

  private final Optional<Integer> instances;

  private final Optional<Boolean> rackSensitive;
  private final Optional<List<String>> rackAffinity;
  private final Optional<SlavePlacement> slavePlacement;

  private final Optional<Boolean> loadBalanced;

  private final Optional<String> group;

  @JsonCreator
  public SingularityRequest(@JsonProperty("id") String id, @JsonProperty("requestType") RequestType requestType, @JsonProperty("owners") Optional<List<String>> owners,
      @JsonProperty("numRetriesOnFailure") Optional<Integer> numRetriesOnFailure, @JsonProperty("schedule") Optional<String> schedule, @JsonProperty("daemon") Optional<Boolean> daemon, @JsonProperty("instances") Optional<Integer> instances,
      @JsonProperty("rackSensitive") Optional<Boolean> rackSensitive, @JsonProperty("loadBalanced") Optional<Boolean> loadBalanced,
      @JsonProperty("killOldNonLongRunningTasksAfterMillis") Optional<Long> killOldNonLongRunningTasksAfterMillis, @JsonProperty("scheduleType") Optional<ScheduleType> scheduleType,
      @JsonProperty("quartzSchedule") Optional<String> quartzSchedule, @JsonProperty("rackAffinity") Optional<List<String>> rackAffinity,
      @JsonProperty("slavePlacement") Optional<SlavePlacement> slavePlacement, @JsonProperty("scheduledExpectedRuntimeMillis") Optional<Long> scheduledExpectedRuntimeMillis,
      @JsonProperty("waitAtLeastMillisAfterTaskFinishesForReschedule") Optional<Long> waitAtLeastMillisAfterTaskFinishesForReschedule, @JsonProperty("group") Optional<String> group) {
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
    this.scheduledExpectedRuntimeMillis = scheduledExpectedRuntimeMillis;
    this.waitAtLeastMillisAfterTaskFinishesForReschedule = waitAtLeastMillisAfterTaskFinishesForReschedule;
    this.group = group;

    if (requestType == null) {
      this.requestType = RequestType.fromDaemonAndScheduleAndLoadBalanced(schedule, daemon, loadBalanced);
    } else {
      this.requestType = requestType;
    }
  }

  public SingularityRequestBuilder toBuilder() {
    return new SingularityRequestBuilder(id, requestType)
    .setLoadBalanced(loadBalanced)
    .setInstances(instances)
    .setNumRetriesOnFailure(numRetriesOnFailure)
    .setOwners(copyOfList(owners))
    .setRackSensitive(rackSensitive)
    .setSchedule(schedule)
    .setKillOldNonLongRunningTasksAfterMillis(killOldNonLongRunningTasksAfterMillis)
    .setScheduleType(scheduleType)
    .setQuartzSchedule(quartzSchedule)
    .setRackAffinity(copyOfList(rackAffinity))
    .setWaitAtLeastMillisAfterTaskFinishesForReschedule(waitAtLeastMillisAfterTaskFinishesForReschedule)
    .setSlavePlacement(slavePlacement)
    .setScheduledExpectedRuntimeMillis(scheduledExpectedRuntimeMillis)
    .setGroup(group);
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

  @Deprecated
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

  public RequestType getRequestType() {
    return requestType;
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

  public Optional<Long> getScheduledExpectedRuntimeMillis() {
    return scheduledExpectedRuntimeMillis;
  }

  @JsonIgnore
  public int getInstancesSafe() {
    return getInstances().or(1);
  }

  @JsonIgnore
  public boolean isScheduled() {
    return requestType == RequestType.SCHEDULED;
  }

  @JsonIgnore
  public String getQuartzScheduleSafe() {
    if (quartzSchedule.isPresent()) {
      return quartzSchedule.get();
    }

    return schedule.get();
  }

  @JsonIgnore
  @Deprecated
  public boolean isDaemon() {
    return daemon.or(Boolean.TRUE).booleanValue();
  }

  @JsonIgnore
  public boolean isLongRunning() {
    return requestType.isLongRunning();
  }

  @JsonIgnore
  public boolean isAlwaysRunning() {
    return requestType.isAlwaysRunning();
  }

  @JsonIgnore
  public boolean isOneOff() {
    return requestType == RequestType.ON_DEMAND;
  }

  @JsonIgnore
  public boolean isDeployable() {
    return requestType.isDeployable();
  }

  @JsonIgnore
  public boolean isRackSensitive() {
    return rackSensitive.or(Boolean.FALSE).booleanValue();
  }

  @JsonIgnore
  public boolean isLoadBalanced() {
    return loadBalanced.or(Boolean.FALSE).booleanValue();
  }

  @JsonIgnore
  public ScheduleType getScheduleTypeSafe() {
    return scheduleType.or(ScheduleType.CRON);
  }

  public Optional<Long> getWaitAtLeastMillisAfterTaskFinishesForReschedule() {
    return waitAtLeastMillisAfterTaskFinishesForReschedule;
  }

  public Optional<String> getGroup() {
    return group;
  }

  @Override
  public String toString() {
    return "SingularityRequest [id=" + id + ", requestType=" + requestType + ", owners=" + owners + ", numRetriesOnFailure=" + numRetriesOnFailure + ", schedule=" + schedule + ", quartzSchedule="
        + quartzSchedule + ", scheduleType=" + scheduleType + ", killOldNonLongRunningTasksAfterMillis=" + killOldNonLongRunningTasksAfterMillis + ", scheduledExpectedRuntimeMillis="
        + scheduledExpectedRuntimeMillis + ", waitAtLeastMillisAfterTaskFinishesForReschedule=" + waitAtLeastMillisAfterTaskFinishesForReschedule + ", daemon=" + daemon + ", instances=" + instances
        + ", rackSensitive=" + rackSensitive + ", rackAffinity=" + rackAffinity + ", slavePlacement=" + slavePlacement + ", loadBalanced=" + loadBalanced + ", group=" + group + "]";
  }

}
