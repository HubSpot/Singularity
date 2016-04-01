package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Optional;

public class SingularityRequestBuilder {

  private final String id;
  private final RequestType requestType;

  private Optional<List<String>> owners;
  private Optional<Integer> numRetriesOnFailure;

  private Optional<String> schedule;
  private Optional<String> quartzSchedule;
  private Optional<ScheduleType> scheduleType;

  private Optional<Long> killOldNonLongRunningTasksAfterMillis;
  private Optional<Long> scheduledExpectedRuntimeMillis;

  private Optional<Long> waitAtLeastMillisAfterTaskFinishesForReschedule;

  private Optional<Integer> instances;
  private Optional<Boolean> skipHealthchecks;

  private Optional<Boolean> rackSensitive;
  private Optional<List<String>> rackAffinity;
  private Optional<SlavePlacement> slavePlacement;
  private Optional<Map<String, String>> requiredSlaveAttributes;
  private Optional<Map<String, String>> allowedSlaveAttributes;

  private Optional<Boolean> loadBalanced;

  private Optional<String> group;
  private Optional<Set<String>> readOnlyGroups;
  private Optional<Boolean> bounceAfterScale;
  private Optional<Map<SingularityEmailType, List<SingularityEmailDestination>>> emailConfigurationOverrides;
  private Optional<Boolean> hideEvenNumberAcrossRacksHint;
  private Optional<String> taskLogErrorRegex;

  public SingularityRequestBuilder(String id, RequestType requestType) {
    this.id = checkNotNull(id, "id cannot be null");
    this.requestType = checkNotNull(requestType, "requestType cannot be null");
    this.owners = Optional.absent();
    this.numRetriesOnFailure = Optional.absent();
    this.schedule = Optional.absent();
    this.scheduleType = Optional.absent();
    this.killOldNonLongRunningTasksAfterMillis = Optional.absent();
    this.instances = Optional.absent();
    this.rackSensitive = Optional.absent();
    this.loadBalanced = Optional.absent();
    this.quartzSchedule = Optional.absent();
    this.rackAffinity = Optional.absent();
    this.slavePlacement = Optional.absent();
    this.requiredSlaveAttributes = Optional.absent();
    this.allowedSlaveAttributes = Optional.absent();
    this.scheduledExpectedRuntimeMillis = Optional.absent();
    this.waitAtLeastMillisAfterTaskFinishesForReschedule = Optional.absent();
    this.group = Optional.absent();
    this.readOnlyGroups = Optional.absent();
    this.bounceAfterScale = Optional.absent();
    this.emailConfigurationOverrides = Optional.absent();
    this.skipHealthchecks = Optional.absent();
    this.hideEvenNumberAcrossRacksHint = Optional.absent();
    this.taskLogErrorRegex = Optional.absent();
  }

  public SingularityRequest build() {
    return new SingularityRequest(id, requestType, owners, numRetriesOnFailure, schedule, instances, rackSensitive, loadBalanced, killOldNonLongRunningTasksAfterMillis, scheduleType, quartzSchedule,
        rackAffinity, slavePlacement, requiredSlaveAttributes, allowedSlaveAttributes, scheduledExpectedRuntimeMillis, waitAtLeastMillisAfterTaskFinishesForReschedule, group, readOnlyGroups,
        bounceAfterScale, skipHealthchecks, emailConfigurationOverrides, Optional.<Boolean>absent(), hideEvenNumberAcrossRacksHint, taskLogErrorRegex);
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  public SingularityRequestBuilder setSkipHealthchecks(Optional<Boolean> skipHealthchecks) {
    this.skipHealthchecks = skipHealthchecks;
    return this;
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

  public Optional<Long> getScheduledExpectedRuntimeMillis() {
    return scheduledExpectedRuntimeMillis;
  }

  public SingularityRequestBuilder setScheduledExpectedRuntimeMillis(Optional<Long> scheduledExpectedRuntimeMillis) {
    this.scheduledExpectedRuntimeMillis = scheduledExpectedRuntimeMillis;
    return this;
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public Optional<Long> getWaitAtLeastMillisAfterTaskFinishesForReschedule() {
    return waitAtLeastMillisAfterTaskFinishesForReschedule;
  }

  public SingularityRequestBuilder setWaitAtLeastMillisAfterTaskFinishesForReschedule(Optional<Long> waitAtLeastMillisAfterTaskFinishesForReschedule) {
    this.waitAtLeastMillisAfterTaskFinishesForReschedule = waitAtLeastMillisAfterTaskFinishesForReschedule;
    return this;
  }

  public Optional<String> getGroup() {
    return group;
  }

  public SingularityRequestBuilder setGroup(Optional<String> group) {
    this.group = group;
    return this;
  }

  public SingularityRequestBuilder setRequiredSlaveAttributes(Optional<Map<String, String>> requiredSlaveAttributes) {
    this.requiredSlaveAttributes = requiredSlaveAttributes;
    return this;
  }

  public SingularityRequestBuilder setAllowedSlaveAttributes(Optional<Map<String, String>> allowedSlaveAttributes) {
    this.allowedSlaveAttributes = allowedSlaveAttributes;
    return this;
  }

  public Optional<Set<String>> getReadOnlyGroups() {
    return readOnlyGroups;
  }

  public SingularityRequestBuilder setReadOnlyGroups(Optional<Set<String>> readOnlyGroups) {
    this.readOnlyGroups = readOnlyGroups;
    return this;
  }

  public Optional<Boolean> getBounceAfterScale() {
    return bounceAfterScale;
  }

  public SingularityRequestBuilder setBounceAfterScale(Optional<Boolean> bounceAfterScale) {
    this.bounceAfterScale = bounceAfterScale;
    return this;
  }

  public Optional<Map<SingularityEmailType, List<SingularityEmailDestination>>> getEmailConfigurationOverrides() {
    return emailConfigurationOverrides;
  }

  public SingularityRequestBuilder setEmailConfigurationOverrides(Optional<Map<SingularityEmailType, List<SingularityEmailDestination>>> emailConfigurationOverrides) {
    this.emailConfigurationOverrides = emailConfigurationOverrides;
    return this;
  }

  public Optional<Boolean> getHideEvenNumberAcrossRacksHint() { return hideEvenNumberAcrossRacksHint; }

  public SingularityRequestBuilder setHideEvenNumberAcrossRacksHint(Optional<Boolean> hideEvenNumberAcrossRacksHint) {
    this.hideEvenNumberAcrossRacksHint = hideEvenNumberAcrossRacksHint;
    return this;
  }

  public Optional<String> getTaskLogErrorRegex() { return taskLogErrorRegex; }

  public SingularityRequestBuilder setTaskLogErrorRegex(Optional<String> taskLogErrorRegex) {
    this.taskLogErrorRegex = taskLogErrorRegex;
    return this;
  }

  @Override
  public String toString() {
    return "SingularityRequestBuilder[" +
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
            ", requiredSlaveAttrbiutes=" + requiredSlaveAttributes +
            ", allowedSlaveAttrbiutes=" + allowedSlaveAttributes +
            ", loadBalanced=" + loadBalanced +
            ", group=" + group +
            ", readOnlyGroups=" + readOnlyGroups +
            ", bounceAfterScale=" + bounceAfterScale +
            ", emailConfigurationOverrides=" + emailConfigurationOverrides +
            ", skipHealthchecks=" + skipHealthchecks +
            ", hideEvenNumberAcrossRacksHint=" + hideEvenNumberAcrossRacksHint +
            ", taskLogErrorRegex=" + taskLogErrorRegex +
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
    SingularityRequestBuilder that = (SingularityRequestBuilder) o;
    return Objects.equals(id, that.id) &&
            Objects.equals(requestType, that.requestType) &&
            Objects.equals(owners, that.owners) &&
            Objects.equals(numRetriesOnFailure, that.numRetriesOnFailure) &&
            Objects.equals(schedule, that.schedule) &&
            Objects.equals(quartzSchedule, that.quartzSchedule) &&
            Objects.equals(scheduleType, that.scheduleType) &&
            Objects.equals(killOldNonLongRunningTasksAfterMillis, that.killOldNonLongRunningTasksAfterMillis) &&
            Objects.equals(scheduledExpectedRuntimeMillis, that.scheduledExpectedRuntimeMillis) &&
            Objects.equals(waitAtLeastMillisAfterTaskFinishesForReschedule, that.waitAtLeastMillisAfterTaskFinishesForReschedule) &&
            Objects.equals(instances, that.instances) &&
            Objects.equals(rackSensitive, that.rackSensitive) &&
            Objects.equals(rackAffinity, that.rackAffinity) &&
            Objects.equals(slavePlacement, that.slavePlacement) &&
            Objects.equals(requiredSlaveAttributes, that.requiredSlaveAttributes) &&
            Objects.equals(allowedSlaveAttributes, that.allowedSlaveAttributes) &&
            Objects.equals(loadBalanced, that.loadBalanced) &&
            Objects.equals(group, that.group) &&
            Objects.equals(readOnlyGroups, that.readOnlyGroups) &&
            Objects.equals(bounceAfterScale, that.bounceAfterScale) &&
            Objects.equals(skipHealthchecks, that.skipHealthchecks) &&
            Objects.equals(emailConfigurationOverrides, that.emailConfigurationOverrides) &&
            Objects.equals(hideEvenNumberAcrossRacksHint, that.hideEvenNumberAcrossRacksHint) &&
            Objects.equals(taskLogErrorRegex, that.taskLogErrorRegex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, requestType, owners, numRetriesOnFailure, schedule, quartzSchedule, scheduleType, killOldNonLongRunningTasksAfterMillis,
        scheduledExpectedRuntimeMillis, waitAtLeastMillisAfterTaskFinishesForReschedule, instances, rackSensitive, rackAffinity, slavePlacement,
        requiredSlaveAttributes, allowedSlaveAttributes, loadBalanced, group, readOnlyGroups, bounceAfterScale, skipHealthchecks, emailConfigurationOverrides,
        hideEvenNumberAcrossRacksHint, taskLogErrorRegex);
  }

}
