package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hubspot.singularity.JsonHelpers.copyOfList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

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

  private final Optional<Integer> instances;
  private final Optional<Boolean> skipHealthchecks;

  private final Optional<Boolean> rackSensitive;
  private final Optional<List<String>> rackAffinity;
  private final Optional<SlavePlacement> slavePlacement;
  private final Optional<Map<String, String>> requiredSlaveAttributes;
  private final Optional<Map<String, String>> allowedSlaveAttributes;

  private final Optional<Boolean> loadBalanced;

  private final Optional<String> group;
  private final Optional<Set<String>> readOnlyGroups;
  private final Optional<Boolean> bounceAfterScale;

  private final Optional<Map<SingularityEmailType, List<SingularityEmailDestination>>> emailConfigurationOverrides;

  private final Optional<Boolean> hideEvenNumberAcrossRacksHint;

  @JsonCreator
  public SingularityRequest(@JsonProperty("id") String id, @JsonProperty("requestType") RequestType requestType, @JsonProperty("owners") Optional<List<String>> owners,
      @JsonProperty("numRetriesOnFailure") Optional<Integer> numRetriesOnFailure, @JsonProperty("schedule") Optional<String> schedule, @JsonProperty("instances") Optional<Integer> instances,
      @JsonProperty("rackSensitive") Optional<Boolean> rackSensitive, @JsonProperty("loadBalanced") Optional<Boolean> loadBalanced,
      @JsonProperty("killOldNonLongRunningTasksAfterMillis") Optional<Long> killOldNonLongRunningTasksAfterMillis, @JsonProperty("scheduleType") Optional<ScheduleType> scheduleType,
      @JsonProperty("quartzSchedule") Optional<String> quartzSchedule, @JsonProperty("rackAffinity") Optional<List<String>> rackAffinity,
      @JsonProperty("slavePlacement") Optional<SlavePlacement> slavePlacement, @JsonProperty("requiredSlaveAttributes") Optional<Map<String, String>> requiredSlaveAttributes,
      @JsonProperty("allowedSlaveAttributes") Optional<Map<String, String>> allowedSlaveAttributes, @JsonProperty("scheduledExpectedRuntimeMillis") Optional<Long> scheduledExpectedRuntimeMillis,
      @JsonProperty("waitAtLeastMillisAfterTaskFinishesForReschedule") Optional<Long> waitAtLeastMillisAfterTaskFinishesForReschedule, @JsonProperty("group") Optional<String> group,
      @JsonProperty("readOnlyGroups") Optional<Set<String>> readOnlyGroups, @JsonProperty("bounceAfterScale") Optional<Boolean> bounceAfterScale,
      @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
      @JsonProperty("emailConfigurationOverrides") Optional<Map<SingularityEmailType, List<SingularityEmailDestination>>> emailConfigurationOverrides,
      @JsonProperty("daemon") @Deprecated Optional<Boolean> daemon, @JsonProperty("hideEvenNumberAcrossRacks") Optional<Boolean> hideEvenNumberAcrossRacksHint) {
    this.id = checkNotNull(id, "id cannot be null");
    this.owners = owners;
    this.numRetriesOnFailure = numRetriesOnFailure;
    this.schedule = schedule;
    this.rackSensitive = rackSensitive;
    this.instances = instances;
    this.loadBalanced = loadBalanced;
    this.killOldNonLongRunningTasksAfterMillis = killOldNonLongRunningTasksAfterMillis;
    this.scheduleType = scheduleType;
    this.quartzSchedule = quartzSchedule;
    this.rackAffinity = rackAffinity;
    this.slavePlacement = slavePlacement;
    this.requiredSlaveAttributes = requiredSlaveAttributes;
    this.allowedSlaveAttributes = allowedSlaveAttributes;
    this.scheduledExpectedRuntimeMillis = scheduledExpectedRuntimeMillis;
    this.waitAtLeastMillisAfterTaskFinishesForReschedule = waitAtLeastMillisAfterTaskFinishesForReschedule;
    this.group = group;
    this.readOnlyGroups = readOnlyGroups;
    this.bounceAfterScale = bounceAfterScale;
    this.emailConfigurationOverrides = emailConfigurationOverrides;
    this.skipHealthchecks = skipHealthchecks;
    this.hideEvenNumberAcrossRacksHint = hideEvenNumberAcrossRacksHint;
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
    .setRequiredSlaveAttributes(requiredSlaveAttributes)
    .setAllowedSlaveAttributes(allowedSlaveAttributes)
    .setScheduledExpectedRuntimeMillis(scheduledExpectedRuntimeMillis)
    .setGroup(group)
    .setReadOnlyGroups(readOnlyGroups)
    .setBounceAfterScale(bounceAfterScale)
    .setEmailConfigurationOverrides(emailConfigurationOverrides)
    .setSkipHealthchecks(skipHealthchecks)
    .setHideEvenNumberAcrossRacksHint(hideEvenNumberAcrossRacksHint);
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

  public Optional<Map<String, String>> getRequiredSlaveAttributes() {
    return requiredSlaveAttributes;
  }

  public Optional<Map<String, String>> getAllowedSlaveAttributes() {
    return allowedSlaveAttributes;
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

  public Optional<Set<String>> getReadOnlyGroups() {
    return readOnlyGroups;
  }

  public Optional<Boolean> getBounceAfterScale() {
    return bounceAfterScale;
  }

  public Optional<Map<SingularityEmailType, List<SingularityEmailDestination>>> getEmailConfigurationOverrides() {
    return emailConfigurationOverrides;
}
  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  public Optional<Boolean> getHideEvenNumberAcrossRacksHint() { return hideEvenNumberAcrossRacksHint; }

  @Override
  public String toString() {
    return "SingularityRequest[" +
            "id='" + id + '\'' +
            ", requestType=" + requestType +
            ", owners=" + owners +
            ", numRetriesOnFailure=" + numRetriesOnFailure +
            ", schedule=" + schedule +
            ", quartzSchedule=" + quartzSchedule +
            ", scheduleType=" + scheduleType +
            ", killOldNonLongRunningTasksAfterMillis=" + killOldNonLongRunningTasksAfterMillis +
            ", scheduledExpectedRuntimeMillis=" + scheduledExpectedRuntimeMillis +
            ", waitAtLeastMillisAfterTaskFinishesForReschedule=" + waitAtLeastMillisAfterTaskFinishesForReschedule +
            ", instances=" + instances +
            ", rackSensitive=" + rackSensitive +
            ", rackAffinity=" + rackAffinity +
            ", slavePlacement=" + slavePlacement +
            ", requiredSlaveAttributes=" + requiredSlaveAttributes +
            ", allowedSlaveAttributes=" + allowedSlaveAttributes +
            ", loadBalanced=" + loadBalanced +
            ", group=" + group +
            ", readOnlyGroups=" + readOnlyGroups +
            ", bounceAfterScale=" + bounceAfterScale +
            ", emailConfigurationOverrides=" + emailConfigurationOverrides +
            ", hideEvenNumberAcrossRacksHint=" + hideEvenNumberAcrossRacksHint +
            ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityRequest request = (SingularityRequest) o;
    return Objects.equals(id, request.id) &&
            Objects.equals(requestType, request.requestType) &&
            Objects.equals(owners, request.owners) &&
            Objects.equals(numRetriesOnFailure, request.numRetriesOnFailure) &&
            Objects.equals(schedule, request.schedule) &&
            Objects.equals(quartzSchedule, request.quartzSchedule) &&
            Objects.equals(scheduleType, request.scheduleType) &&
            Objects.equals(killOldNonLongRunningTasksAfterMillis, request.killOldNonLongRunningTasksAfterMillis) &&
            Objects.equals(scheduledExpectedRuntimeMillis, request.scheduledExpectedRuntimeMillis) &&
            Objects.equals(waitAtLeastMillisAfterTaskFinishesForReschedule, request.waitAtLeastMillisAfterTaskFinishesForReschedule) &&
            Objects.equals(instances, request.instances) &&
            Objects.equals(rackSensitive, request.rackSensitive) &&
            Objects.equals(rackAffinity, request.rackAffinity) &&
            Objects.equals(slavePlacement, request.slavePlacement) &&
            Objects.equals(requiredSlaveAttributes, request.requiredSlaveAttributes) &&
            Objects.equals(allowedSlaveAttributes, request.allowedSlaveAttributes) &&
            Objects.equals(loadBalanced, request.loadBalanced) &&
            Objects.equals(group, request.group) &&
            Objects.equals(readOnlyGroups, request.readOnlyGroups) &&
            Objects.equals(bounceAfterScale, request.bounceAfterScale) &&
            Objects.equals(emailConfigurationOverrides, request.emailConfigurationOverrides) &&
            Objects.equals(hideEvenNumberAcrossRacksHint, request.hideEvenNumberAcrossRacksHint);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, requestType, owners, numRetriesOnFailure, schedule, quartzSchedule, scheduleType, killOldNonLongRunningTasksAfterMillis, scheduledExpectedRuntimeMillis, waitAtLeastMillisAfterTaskFinishesForReschedule, instances, rackSensitive, rackAffinity, slavePlacement, requiredSlaveAttributes, allowedSlaveAttributes, loadBalanced, group, readOnlyGroups, bounceAfterScale, emailConfigurationOverrides, hideEvenNumberAcrossRacksHint);
  }
}
