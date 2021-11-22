package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SingularityRequestBuilder {
  private String id;
  private RequestType requestType;

  private Optional<List<String>> owners;
  private Optional<Integer> numRetriesOnFailure;
  private Optional<Integer> maxScale;

  private Optional<String> schedule;
  private Optional<String> quartzSchedule;
  private Optional<String> scheduleTimeZone;
  private Optional<ScheduleType> scheduleType;

  private Optional<Long> killOldNonLongRunningTasksAfterMillis;
  private Optional<Long> taskExecutionTimeLimitMillis;
  private Optional<Long> scheduledExpectedRuntimeMillis;

  private Optional<Long> waitAtLeastMillisAfterTaskFinishesForReschedule;

  private Optional<Integer> instances;
  private Optional<Boolean> skipHealthchecks;

  private Optional<Boolean> rackSensitive;

  private Optional<List<String>> rackAffinity;
  private Optional<AgentPlacement> agentPlacement;
  private Optional<Map<String, String>> requiredAgentAttributes;
  private Optional<Map<String, String>> allowedAgentAttributes;
  private Optional<Map<String, Map<String, Integer>>> agentAttributeMinimums;
  private Optional<Boolean> loadBalanced;
  private Optional<String> requiredRole;

  private Optional<String> group;
  private Optional<Set<String>> readWriteGroups;
  private Optional<Set<String>> readOnlyGroups;
  private Optional<Map<String, Set<SingularityUserFacingAction>>> actionPermissions;
  private Optional<Boolean> bounceAfterScale;
  private Optional<Map<SingularityEmailType, List<SingularityEmailDestination>>> emailConfigurationOverrides;
  private Optional<Boolean> hideEvenNumberAcrossRacksHint;
  private Optional<String> taskLogErrorRegex;
  private Optional<Boolean> taskLogErrorRegexCaseSensitive;
  private Optional<Double> taskPriorityLevel;
  private Optional<Integer> maxTasksPerOffer;
  private Optional<Boolean> allowBounceToSameHost;

  @Deprecated
  private Optional<String> dataCenter;

  public SingularityRequestBuilder(String id, RequestType requestType) {
    this.id = checkNotNull(id, "id cannot be null");
    this.requestType = checkNotNull(requestType, "requestType cannot be null");
    this.owners = Optional.empty();
    this.numRetriesOnFailure = Optional.empty();
    this.maxScale = Optional.empty();
    this.schedule = Optional.empty();
    this.scheduleType = Optional.empty();
    this.killOldNonLongRunningTasksAfterMillis = Optional.empty();
    this.taskExecutionTimeLimitMillis = Optional.empty();
    this.instances = Optional.empty();
    this.rackSensitive = Optional.empty();
    this.loadBalanced = Optional.empty();
    this.quartzSchedule = Optional.empty();
    this.scheduleTimeZone = Optional.empty();
    this.rackAffinity = Optional.empty();
    this.agentPlacement = Optional.empty();
    this.requiredAgentAttributes = Optional.empty();
    this.allowedAgentAttributes = Optional.empty();
    this.agentAttributeMinimums = Optional.empty();
    this.scheduledExpectedRuntimeMillis = Optional.empty();
    this.waitAtLeastMillisAfterTaskFinishesForReschedule = Optional.empty();
    this.group = Optional.empty();
    this.readWriteGroups = Optional.empty();
    this.readOnlyGroups = Optional.empty();
    this.actionPermissions = Optional.empty();
    this.bounceAfterScale = Optional.empty();
    this.emailConfigurationOverrides = Optional.empty();
    this.skipHealthchecks = Optional.empty();
    this.hideEvenNumberAcrossRacksHint = Optional.empty();
    this.taskLogErrorRegex = Optional.empty();
    this.taskLogErrorRegexCaseSensitive = Optional.empty();
    this.taskPriorityLevel = Optional.empty();
    this.maxTasksPerOffer = Optional.empty();
    this.allowBounceToSameHost = Optional.empty();
    this.requiredRole = Optional.empty();
    this.dataCenter = Optional.empty();
  }

  public SingularityRequest build() {
    return new SingularityRequest(
      id,
      requestType,
      owners,
      numRetriesOnFailure,
      maxScale,
      schedule,
      instances,
      rackSensitive,
      loadBalanced,
      killOldNonLongRunningTasksAfterMillis,
      taskExecutionTimeLimitMillis,
      scheduleType,
      quartzSchedule,
      scheduleTimeZone,
      rackAffinity,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      scheduledExpectedRuntimeMillis,
      waitAtLeastMillisAfterTaskFinishesForReschedule,
      group,
      readWriteGroups,
      readOnlyGroups,
      actionPermissions,
      bounceAfterScale,
      skipHealthchecks,
      emailConfigurationOverrides,
      Optional.<Boolean>empty(),
      hideEvenNumberAcrossRacksHint,
      taskLogErrorRegex,
      taskLogErrorRegexCaseSensitive,
      taskPriorityLevel,
      maxTasksPerOffer,
      allowBounceToSameHost,
      requiredRole,
      dataCenter,
      requiredAgentAttributes,
      allowedAgentAttributes,
      agentAttributeMinimums,
      agentPlacement
    );
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  public SingularityRequestBuilder setSkipHealthchecks(
    Optional<Boolean> skipHealthchecks
  ) {
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

  public SingularityRequestBuilder setNumRetriesOnFailure(
    Optional<Integer> numRetriesOnFailure
  ) {
    this.numRetriesOnFailure = numRetriesOnFailure;
    return this;
  }

  public Optional<Integer> getMaxScale() {
    return maxScale;
  }

  public SingularityRequestBuilder setMaxScale(Optional<Integer> maxScale) {
    this.maxScale = maxScale;
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

  public SingularityRequestBuilder setRequiredRole(Optional<String> requiredRole) {
    this.requiredRole = requiredRole;
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

  public SingularityRequestBuilder setKillOldNonLongRunningTasksAfterMillis(
    Optional<Long> killOldNonLongRunningTasksAfterMillis
  ) {
    this.killOldNonLongRunningTasksAfterMillis = killOldNonLongRunningTasksAfterMillis;
    return this;
  }

  public Optional<Long> getTaskExecutionTimeLimitMillis() {
    return taskExecutionTimeLimitMillis;
  }

  public SingularityRequestBuilder setTaskExecutionTimeLimitMillis(
    Optional<Long> taskExecutionTimeLimitMillis
  ) {
    this.taskExecutionTimeLimitMillis = taskExecutionTimeLimitMillis;
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

  public Optional<String> getScheduleTimeZone() {
    return scheduleTimeZone;
  }

  public SingularityRequestBuilder setScheduleTimeZone(
    Optional<String> scheduleTimeZone
  ) {
    this.scheduleTimeZone = scheduleTimeZone;
    return this;
  }

  public Optional<List<String>> getRackAffinity() {
    return rackAffinity;
  }

  public SingularityRequestBuilder setRackAffinity(Optional<List<String>> rackAffinity) {
    this.rackAffinity = rackAffinity;
    return this;
  }

  public Optional<AgentPlacement> getAgentPlacement() {
    return agentPlacement;
  }

  public SingularityRequestBuilder setAgentPlacement(
    Optional<AgentPlacement> agentPlacement
  ) {
    this.agentPlacement = agentPlacement;
    return this;
  }

  @Deprecated
  public Optional<SlavePlacement> getSlavePlacement() {
    return agentPlacement.map(a -> SlavePlacement.valueOf(a.name()));
  }

  @Deprecated
  public SingularityRequestBuilder setSlavePlacement(
    Optional<SlavePlacement> agentPlacement
  ) {
    this.agentPlacement = agentPlacement.map(s -> AgentPlacement.valueOf(s.name()));
    return this;
  }

  public Optional<Long> getScheduledExpectedRuntimeMillis() {
    return scheduledExpectedRuntimeMillis;
  }

  public SingularityRequestBuilder setScheduledExpectedRuntimeMillis(
    Optional<Long> scheduledExpectedRuntimeMillis
  ) {
    this.scheduledExpectedRuntimeMillis = scheduledExpectedRuntimeMillis;
    return this;
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public SingularityRequestBuilder setRequestType(RequestType requestType) {
    this.requestType = requestType;
    return this;
  }

  public Optional<Long> getWaitAtLeastMillisAfterTaskFinishesForReschedule() {
    return waitAtLeastMillisAfterTaskFinishesForReschedule;
  }

  public SingularityRequestBuilder setWaitAtLeastMillisAfterTaskFinishesForReschedule(
    Optional<Long> waitAtLeastMillisAfterTaskFinishesForReschedule
  ) {
    this.waitAtLeastMillisAfterTaskFinishesForReschedule =
      waitAtLeastMillisAfterTaskFinishesForReschedule;
    return this;
  }

  public Optional<String> getGroup() {
    return group;
  }

  public SingularityRequestBuilder setGroup(Optional<String> group) {
    this.group = group;
    return this;
  }

  public Optional<Set<String>> getReadWriteGroups() {
    return readWriteGroups;
  }

  public SingularityRequestBuilder setReadWriteGroups(
    Optional<Set<String>> readWriteGroups
  ) {
    this.readWriteGroups = readWriteGroups;
    return this;
  }

  public Optional<Map<String, Set<SingularityUserFacingAction>>> getActionPermissions() {
    return actionPermissions;
  }

  public SingularityRequestBuilder setActionPermissions(
    Optional<Map<String, Set<SingularityUserFacingAction>>> getActionPermissions
  ) {
    this.actionPermissions = getActionPermissions;
    return this;
  }

  public SingularityRequestBuilder setRequiredAgentAttributes(
    Optional<Map<String, String>> requiredAgentAttributes
  ) {
    this.requiredAgentAttributes = requiredAgentAttributes;
    return this;
  }

  public SingularityRequestBuilder setAllowedAgentAttributes(
    Optional<Map<String, String>> allowedAgentAttributes
  ) {
    this.allowedAgentAttributes = allowedAgentAttributes;
    return this;
  }

  public SingularityRequestBuilder setAgentAttributeMinimums(
    Optional<Map<String, Map<String, Integer>>> agentAttributeMinimums
  ) {
    this.agentAttributeMinimums = agentAttributeMinimums;
    return this;
  }

  @Deprecated
  public SingularityRequestBuilder setRequiredSlaveAttributes(
    Optional<Map<String, String>> requiredAgentAttributes
  ) {
    this.requiredAgentAttributes = requiredAgentAttributes;
    return this;
  }

  @Deprecated
  public SingularityRequestBuilder setAllowedSlaveAttributes(
    Optional<Map<String, String>> allowedAgentAttributes
  ) {
    this.allowedAgentAttributes = allowedAgentAttributes;
    return this;
  }

  @Deprecated
  public SingularityRequestBuilder setSlaveAttributeMinimums(
    Optional<Map<String, Map<String, Integer>>> agentAttributeMinimums
  ) {
    this.agentAttributeMinimums = agentAttributeMinimums;
    return this;
  }

  public Optional<Set<String>> getReadOnlyGroups() {
    return readOnlyGroups;
  }

  public SingularityRequestBuilder setReadOnlyGroups(
    Optional<Set<String>> readOnlyGroups
  ) {
    this.readOnlyGroups = readOnlyGroups;
    return this;
  }

  public Optional<Boolean> getBounceAfterScale() {
    return bounceAfterScale;
  }

  public SingularityRequestBuilder setBounceAfterScale(
    Optional<Boolean> bounceAfterScale
  ) {
    this.bounceAfterScale = bounceAfterScale;
    return this;
  }

  public Optional<Map<SingularityEmailType, List<SingularityEmailDestination>>> getEmailConfigurationOverrides() {
    return emailConfigurationOverrides;
  }

  public SingularityRequestBuilder setEmailConfigurationOverrides(
    Optional<Map<SingularityEmailType, List<SingularityEmailDestination>>> emailConfigurationOverrides
  ) {
    this.emailConfigurationOverrides = emailConfigurationOverrides;
    return this;
  }

  public Optional<Boolean> getHideEvenNumberAcrossRacksHint() {
    return hideEvenNumberAcrossRacksHint;
  }

  public SingularityRequestBuilder setHideEvenNumberAcrossRacksHint(
    Optional<Boolean> hideEvenNumberAcrossRacksHint
  ) {
    this.hideEvenNumberAcrossRacksHint = hideEvenNumberAcrossRacksHint;
    return this;
  }

  public Optional<String> getTaskLogErrorRegex() {
    return taskLogErrorRegex;
  }

  public SingularityRequestBuilder setTaskLogErrorRegex(
    Optional<String> taskLogErrorRegex
  ) {
    this.taskLogErrorRegex = taskLogErrorRegex;
    return this;
  }

  public Optional<Boolean> getTaskLogErrorRegexCaseSensitive() {
    return taskLogErrorRegexCaseSensitive;
  }

  public SingularityRequestBuilder setTaskLogErrorRegexCaseSensitive(
    Optional<Boolean> taskLogErrorRegexCaseSensitive
  ) {
    this.taskLogErrorRegexCaseSensitive = taskLogErrorRegexCaseSensitive;
    return this;
  }

  public Optional<Double> getTaskPriorityLevel() {
    return taskPriorityLevel;
  }

  public SingularityRequestBuilder setTaskPriorityLevel(
    Optional<Double> taskPriorityLevel
  ) {
    this.taskPriorityLevel = taskPriorityLevel;
    return this;
  }

  public Optional<Integer> getMaxTasksPerOffer() {
    return maxTasksPerOffer;
  }

  public SingularityRequestBuilder setMaxTasksPerOffer(
    Optional<Integer> maxTasksPerOffer
  ) {
    this.maxTasksPerOffer = maxTasksPerOffer;
    return this;
  }

  public Optional<Boolean> getAllowBounceToSameHost() {
    return allowBounceToSameHost;
  }

  public SingularityRequestBuilder setAllowBounceToSameHost(
    Optional<Boolean> allowBounceToSameHost
  ) {
    this.allowBounceToSameHost = allowBounceToSameHost;
    return this;
  }

  @Deprecated
  public Optional<String> getDataCenter() {
    return dataCenter;
  }

  @Deprecated
  public SingularityRequestBuilder setDataCenter(Optional<String> dataCenter) {
    this.dataCenter = dataCenter;
    return this;
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
    return (
      Objects.equals(id, that.id) &&
      requestType == that.requestType &&
      Objects.equals(owners, that.owners) &&
      Objects.equals(numRetriesOnFailure, that.numRetriesOnFailure) &&
      Objects.equals(maxScale, that.maxScale) &&
      Objects.equals(schedule, that.schedule) &&
      Objects.equals(quartzSchedule, that.quartzSchedule) &&
      Objects.equals(scheduleTimeZone, that.scheduleTimeZone) &&
      Objects.equals(scheduleType, that.scheduleType) &&
      Objects.equals(
        killOldNonLongRunningTasksAfterMillis,
        that.killOldNonLongRunningTasksAfterMillis
      ) &&
      Objects.equals(taskExecutionTimeLimitMillis, that.taskExecutionTimeLimitMillis) &&
      Objects.equals(
        scheduledExpectedRuntimeMillis,
        that.scheduledExpectedRuntimeMillis
      ) &&
      Objects.equals(
        waitAtLeastMillisAfterTaskFinishesForReschedule,
        that.waitAtLeastMillisAfterTaskFinishesForReschedule
      ) &&
      Objects.equals(instances, that.instances) &&
      Objects.equals(skipHealthchecks, that.skipHealthchecks) &&
      Objects.equals(rackSensitive, that.rackSensitive) &&
      Objects.equals(rackAffinity, that.rackAffinity) &&
      Objects.equals(agentPlacement, that.agentPlacement) &&
      Objects.equals(requiredAgentAttributes, that.requiredAgentAttributes) &&
      Objects.equals(allowedAgentAttributes, that.allowedAgentAttributes) &&
      Objects.equals(agentAttributeMinimums, that.agentAttributeMinimums) &&
      Objects.equals(loadBalanced, that.loadBalanced) &&
      Objects.equals(requiredRole, that.requiredRole) &&
      Objects.equals(group, that.group) &&
      Objects.equals(readWriteGroups, that.readWriteGroups) &&
      Objects.equals(readOnlyGroups, that.readOnlyGroups) &&
      Objects.equals(actionPermissions, that.actionPermissions) &&
      Objects.equals(bounceAfterScale, that.bounceAfterScale) &&
      Objects.equals(emailConfigurationOverrides, that.emailConfigurationOverrides) &&
      Objects.equals(hideEvenNumberAcrossRacksHint, that.hideEvenNumberAcrossRacksHint) &&
      Objects.equals(taskLogErrorRegex, that.taskLogErrorRegex) &&
      Objects.equals(
        taskLogErrorRegexCaseSensitive,
        that.taskLogErrorRegexCaseSensitive
      ) &&
      Objects.equals(taskPriorityLevel, that.taskPriorityLevel) &&
      Objects.equals(maxTasksPerOffer, that.maxTasksPerOffer) &&
      Objects.equals(allowBounceToSameHost, that.allowBounceToSameHost) &&
      Objects.equals(dataCenter, that.dataCenter)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      id,
      requestType,
      owners,
      numRetriesOnFailure,
      maxScale,
      schedule,
      quartzSchedule,
      scheduleTimeZone,
      scheduleType,
      killOldNonLongRunningTasksAfterMillis,
      taskExecutionTimeLimitMillis,
      scheduledExpectedRuntimeMillis,
      waitAtLeastMillisAfterTaskFinishesForReschedule,
      instances,
      skipHealthchecks,
      rackSensitive,
      rackAffinity,
      agentPlacement,
      requiredAgentAttributes,
      allowedAgentAttributes,
      agentAttributeMinimums,
      loadBalanced,
      requiredRole,
      group,
      readWriteGroups,
      readOnlyGroups,
      actionPermissions,
      bounceAfterScale,
      emailConfigurationOverrides,
      hideEvenNumberAcrossRacksHint,
      taskLogErrorRegex,
      taskLogErrorRegexCaseSensitive,
      taskPriorityLevel,
      maxTasksPerOffer,
      allowBounceToSameHost,
      dataCenter
    );
  }

  @Override
  public String toString() {
    return (
      "SingularityRequestBuilder{" +
      "id='" +
      id +
      '\'' +
      ", requestType=" +
      requestType +
      ", owners=" +
      owners +
      ", numRetriesOnFailure=" +
      numRetriesOnFailure +
      ", maxScale=" +
      maxScale +
      ", schedule=" +
      schedule +
      ", quartzSchedule=" +
      quartzSchedule +
      ", scheduleTimeZone=" +
      scheduleTimeZone +
      ", scheduleType=" +
      scheduleType +
      ", killOldNonLongRunningTasksAfterMillis=" +
      killOldNonLongRunningTasksAfterMillis +
      ", taskExecutionTimeLimitMillis=" +
      taskExecutionTimeLimitMillis +
      ", scheduledExpectedRuntimeMillis=" +
      scheduledExpectedRuntimeMillis +
      ", waitAtLeastMillisAfterTaskFinishesForReschedule=" +
      waitAtLeastMillisAfterTaskFinishesForReschedule +
      ", instances=" +
      instances +
      ", skipHealthchecks=" +
      skipHealthchecks +
      ", rackSensitive=" +
      rackSensitive +
      ", rackAffinity=" +
      rackAffinity +
      ", agentPlacement=" +
      agentPlacement +
      ", requiredAgentAttributes=" +
      requiredAgentAttributes +
      ", allowedAgentAttributes=" +
      allowedAgentAttributes +
      ", agentAttributeMinimums=" +
      agentAttributeMinimums +
      ", loadBalanced=" +
      loadBalanced +
      ", requiredRole=" +
      requiredRole +
      ", group=" +
      group +
      ", readWriteGroups=" +
      readWriteGroups +
      ", readOnlyGroups=" +
      readOnlyGroups +
      ", actionPermissions=" +
      actionPermissions +
      ", bounceAfterScale=" +
      bounceAfterScale +
      ", emailConfigurationOverrides=" +
      emailConfigurationOverrides +
      ", hideEvenNumberAcrossRacksHint=" +
      hideEvenNumberAcrossRacksHint +
      ", taskLogErrorRegex=" +
      taskLogErrorRegex +
      ", taskLogErrorRegexCaseSensitive=" +
      taskLogErrorRegexCaseSensitive +
      ", taskPriorityLevel=" +
      taskPriorityLevel +
      ", maxTasksPerOffer=" +
      maxTasksPerOffer +
      ", allowBounceToSameHost=" +
      allowBounceToSameHost +
      ", dataCenter=" +
      dataCenter +
      '}'
    );
  }
}
