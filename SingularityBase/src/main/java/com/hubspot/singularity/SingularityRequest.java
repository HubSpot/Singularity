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
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Settings that apply to all tasks and deploys assocaited with this request")
public class SingularityRequest {

  private final String id;
  private final RequestType requestType;
  private final Optional<List<String>> owners;
  private final Optional<Integer> numRetriesOnFailure;
  private final Optional<String> schedule;
  private final Optional<String> quartzSchedule;
  private final Optional<ScheduleType> scheduleType;
  private final Optional<String> scheduleTimeZone;
  private final Optional<Long> killOldNonLongRunningTasksAfterMillis;
  private final Optional<Long> taskExecutionTimeLimitMillis;
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
  private final Optional<String> requiredRole;
  private final Optional<Set<String>> readWriteGroups;
  private final Optional<Set<String>> readOnlyGroups;
  private final Optional<Boolean> bounceAfterScale;
  private final Optional<Map<SingularityEmailType, List<SingularityEmailDestination>>> emailConfigurationOverrides;
  private final Optional<Boolean> hideEvenNumberAcrossRacksHint;
  private final Optional<String> taskLogErrorRegex;
  private final Optional<Boolean> taskLogErrorRegexCaseSensitive;
  private final Optional<Double> taskPriorityLevel;
  private final Optional<Integer> maxTasksPerOffer;
  private final Optional<Boolean> allowBounceToSameHost;
  @Deprecated
  private final Optional<String> dataCenter;

  @JsonCreator
  public SingularityRequest(@JsonProperty("id") String id,
                            @JsonProperty("requestType") RequestType requestType,
                            @JsonProperty("owners") Optional<List<String>> owners,
                            @JsonProperty("numRetriesOnFailure") Optional<Integer> numRetriesOnFailure,
                            @JsonProperty("schedule") Optional<String> schedule,
                            @JsonProperty("instances") Optional<Integer> instances,
                            @JsonProperty("rackSensitive") Optional<Boolean> rackSensitive,
                            @JsonProperty("loadBalanced") Optional<Boolean> loadBalanced,
                            @JsonProperty("killOldNonLongRunningTasksAfterMillis") Optional<Long> killOldNonLongRunningTasksAfterMillis,
                            @JsonProperty("taskExecutionTimeLimitMillis") Optional<Long> taskExecutionTimeLimitMillis,
                            @JsonProperty("scheduleType") Optional<ScheduleType> scheduleType,
                            @JsonProperty("quartzSchedule") Optional<String> quartzSchedule,
                            @JsonProperty("scheduleTimeZone") Optional<String> scheduleTimeZone,
                            @JsonProperty("rackAffinity") Optional<List<String>> rackAffinity,
                            @JsonProperty("slavePlacement") Optional<SlavePlacement> slavePlacement,
                            @JsonProperty("requiredSlaveAttributes") Optional<Map<String, String>> requiredSlaveAttributes,
                            @JsonProperty("allowedSlaveAttributes") Optional<Map<String, String>> allowedSlaveAttributes,
                            @JsonProperty("scheduledExpectedRuntimeMillis") Optional<Long> scheduledExpectedRuntimeMillis,
                            @JsonProperty("waitAtLeastMillisAfterTaskFinishesForReschedule") Optional<Long> waitAtLeastMillisAfterTaskFinishesForReschedule,
                            @JsonProperty("group") Optional<String> group,
                            @JsonProperty("readWriteGroups") Optional<Set<String>> readWriteGroups,
                            @JsonProperty("readOnlyGroups") Optional<Set<String>> readOnlyGroups,
                            @JsonProperty("bounceAfterScale") Optional<Boolean> bounceAfterScale,
                            @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
                            @JsonProperty("emailConfigurationOverrides") Optional<Map<SingularityEmailType, List<SingularityEmailDestination>>> emailConfigurationOverrides,
                            @JsonProperty("daemon") @Deprecated Optional<Boolean> daemon,
                            @JsonProperty("hideEvenNumberAcrossRacks") Optional<Boolean> hideEvenNumberAcrossRacksHint,
                            @JsonProperty("taskLogErrorRegex") Optional<String> taskLogErrorRegex,
                            @JsonProperty("taskLogErrorRegexCaseSensitive") Optional<Boolean> taskLogErrorRegexCaseSensitive,
                            @JsonProperty("taskPriorityLevel") Optional<Double> taskPriorityLevel,
                            @JsonProperty("maxTasksPerOffer") Optional<Integer> maxTasksPerOffer,
                            @JsonProperty("allowBounceToSameHost") Optional<Boolean> allowBounceToSameHost,
                            @JsonProperty("requiredRole") Optional<String> requiredRole,
                            @JsonProperty("dataCenter") Optional<String> dataCenter) {
    this.id = checkNotNull(id, "id cannot be null");
    this.owners = owners;
    this.numRetriesOnFailure = numRetriesOnFailure;
    this.schedule = schedule;
    this.rackSensitive = rackSensitive;
    this.instances = instances;
    this.loadBalanced = loadBalanced;
    this.killOldNonLongRunningTasksAfterMillis = killOldNonLongRunningTasksAfterMillis;
    this.taskExecutionTimeLimitMillis = taskExecutionTimeLimitMillis;
    this.scheduleType = scheduleType;
    this.quartzSchedule = quartzSchedule;
    this.scheduleTimeZone = scheduleTimeZone;
    this.rackAffinity = rackAffinity;
    this.slavePlacement = slavePlacement;
    this.requiredSlaveAttributes = requiredSlaveAttributes;
    this.allowedSlaveAttributes = allowedSlaveAttributes;
    this.scheduledExpectedRuntimeMillis = scheduledExpectedRuntimeMillis;
    this.waitAtLeastMillisAfterTaskFinishesForReschedule = waitAtLeastMillisAfterTaskFinishesForReschedule;
    this.group = group;
    this.requiredRole = requiredRole;
    this.readWriteGroups = readWriteGroups;
    this.readOnlyGroups = readOnlyGroups;
    this.bounceAfterScale = bounceAfterScale;
    this.emailConfigurationOverrides = emailConfigurationOverrides;
    this.skipHealthchecks = skipHealthchecks;
    this.hideEvenNumberAcrossRacksHint = hideEvenNumberAcrossRacksHint;
    this.taskLogErrorRegex = taskLogErrorRegex;
    this.taskLogErrorRegexCaseSensitive = taskLogErrorRegexCaseSensitive;
    this.taskPriorityLevel = taskPriorityLevel;
    this.maxTasksPerOffer = maxTasksPerOffer;
    this.allowBounceToSameHost = allowBounceToSameHost;
    if (requestType == null) {
      this.requestType = RequestType.fromDaemonAndScheduleAndLoadBalanced(schedule, daemon, loadBalanced);
    } else {
      this.requestType = requestType;
    }
    this.dataCenter = dataCenter;
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
    .setTaskExecutionTimeLimitMillis(taskExecutionTimeLimitMillis)
    .setScheduleType(scheduleType)
    .setQuartzSchedule(quartzSchedule)
    .setScheduleTimeZone(scheduleTimeZone)
    .setRackAffinity(copyOfList(rackAffinity))
    .setWaitAtLeastMillisAfterTaskFinishesForReschedule(waitAtLeastMillisAfterTaskFinishesForReschedule)
    .setSlavePlacement(slavePlacement)
    .setRequiredSlaveAttributes(requiredSlaveAttributes)
    .setAllowedSlaveAttributes(allowedSlaveAttributes)
    .setScheduledExpectedRuntimeMillis(scheduledExpectedRuntimeMillis)
    .setRequiredRole(requiredRole)
    .setGroup(group)
    .setReadWriteGroups(readWriteGroups)
    .setReadOnlyGroups(readOnlyGroups)
    .setBounceAfterScale(bounceAfterScale)
    .setEmailConfigurationOverrides(emailConfigurationOverrides)
    .setSkipHealthchecks(skipHealthchecks)
    .setHideEvenNumberAcrossRacksHint(hideEvenNumberAcrossRacksHint)
    .setTaskLogErrorRegex(taskLogErrorRegex)
    .setTaskLogErrorRegexCaseSensitive(taskLogErrorRegexCaseSensitive)
    .setTaskPriorityLevel(taskPriorityLevel)
    .setMaxTasksPerOffer(maxTasksPerOffer)
    .setAllowBounceToSameHost(allowBounceToSameHost)
    .setDataCenter(dataCenter);
  }

  @Schema(
      required = true,
      title = "A unique id for the request",
      pattern = "a-zA-Z0-9_-",
      description = "Max length is set in configuration yaml as maxRequestIdSize"
  )
  public String getId() {
    return id;
  }

  @Schema(nullable = true, description = "A list of emails for the owners of this request")
  public Optional<List<String>> getOwners() {
    return owners;
  }

  @Schema(nullable = true, description = "For scheduled jobs, retry up to this many times if the job fails")
  public Optional<Integer> getNumRetriesOnFailure() {
    return numRetriesOnFailure;
  }

  @Schema(nullable = true, description = "A schedule in cron, RFC5545, or quartz format")
  public Optional<String> getSchedule() {
    return schedule;
  }

  @Schema(nullable = true, description = "A schedule in quartz format")
  public Optional<String> getQuartzSchedule() {
    return quartzSchedule;
  }

  @Schema(nullable = true, description = "Time zone to use when running the")
  public Optional<String> getScheduleTimeZone() {
    return scheduleTimeZone;
  }

  @Schema(nullable = true, description = "A count of tasks to run for long-running requests")
  public Optional<Integer> getInstances() {
    return instances;
  }

  @Schema(nullable = true, description = "Spread instances for this request evenly across separate racks")
  public Optional<Boolean> getRackSensitive() {
    return rackSensitive;
  }

  @Schema(nullable = true, description = "Indicates that a SERVICE should be load balanced")
  public Optional<Boolean> getLoadBalanced() {
    return loadBalanced;
  }

  @Schema(required=true, title = "The type of request, can be SERVICE, WORKER, SCHEDULED, ON_DEMAND, or RUN_ONCE")
  public RequestType getRequestType() {
    return requestType;
  }

  @Schema(nullable = true, description = "For non-long-running request types, kill a task after this amount of time if it has been put into CLEANING and has not shut down")
  public Optional<Long> getKillOldNonLongRunningTasksAfterMillis() {
    return killOldNonLongRunningTasksAfterMillis;
  }

  @Schema(nullable = true, description = "If set, don't allow any taks for this request to run for longer than this amount of time")
  public Optional<Long> getTaskExecutionTimeLimitMillis() {
    return taskExecutionTimeLimitMillis;
  }

  @Schema(nullable = true, description = "The type of schedule associated with the scheduled field. Can be CRON, QUARTZ, or RFC5545")
  public Optional<ScheduleType> getScheduleType() {
    return scheduleType;
  }

  @Schema(nullable = true, description = "If set, prefer this specific rack when launching tasks")
  public Optional<List<String>> getRackAffinity() {
    return rackAffinity;
  }

  @Schema(nullable = true, description = "Strategy for determining where to place new tasks. Can be SEPARATE, OPTIMISTIC, GREEDY, SEPARATE_BY_DEPLOY, or SEPARATE_BY_REQUEST")
  public Optional<SlavePlacement> getSlavePlacement() {
    return slavePlacement;
  }

  @Schema(nullable = true, description = "Expected time for a non-long-running task to run. Singularity will notify owners if a task exceeds this time")
  public Optional<Long> getScheduledExpectedRuntimeMillis() {
    return scheduledExpectedRuntimeMillis;
  }

  @Schema(nullable = true, description = "Only allow tasks for this request to run on slaves which have these attributes")
  public Optional<Map<String, String>> getRequiredSlaveAttributes() {
    return requiredSlaveAttributes;
  }

  @Schema(nullable = true, description = "Allow tasks to run on slaves with these attributes, but do not restrict them to only these slaves")
  public Optional<Map<String, String>> getAllowedSlaveAttributes() {
    return allowedSlaveAttributes;
  }

  @Schema(nullable = true, description = "Do not schedule more than this many tasks using a single offer from a single mesos slave")
  public Optional<Integer> getMaxTasksPerOffer() {
    return maxTasksPerOffer;
  }

  @Schema(nullable = true, description = "If set to true, allow tasks to be scheduled on the same host as an existing active task when bouncing")
  public Optional<Boolean> getAllowBounceToSameHost() {
    return allowBounceToSameHost;
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
    return rackSensitive.or(false);
  }

  @JsonIgnore
  public boolean isLoadBalanced() {
    return loadBalanced.or(false);
  }

  @JsonIgnore
  public ScheduleType getScheduleTypeSafe() {
    return scheduleType.or(ScheduleType.CRON);
  }

  @Schema(nullable = true, description = "When a scheduled job finishes, wait at least this long before rescheduling it")
  public Optional<Long> getWaitAtLeastMillisAfterTaskFinishesForReschedule() {
    return waitAtLeastMillisAfterTaskFinishesForReschedule;
  }

  @Schema(nullable = true, description = "Auth group associated with this request. Users in this group are allowed read/write access to this request")
  public Optional<String> getGroup() {
    return group;
  }

  @Schema(nullable = true, description = "Mesos Role required for this request. Only offers with the required role will be accepted to execute the tasks associated with the request")
  public Optional<String> getRequiredRole() {
    return requiredRole;
  }

  @Schema(nullable = true, description = "Users in these groups are allowed read/write access to this request")
  public Optional<Set<String>> getReadWriteGroups() {
    return readWriteGroups;
  }

  @Schema(nullable = true, description = "Users in these groups are allowed read only access to this request")
  public Optional<Set<String>> getReadOnlyGroups() {
    return readOnlyGroups;
  }

  @Schema(nullable = true, description = "Used for SingularityUI. If true, automatically trigger a bounce after changing the request's instance count")
  public Optional<Boolean> getBounceAfterScale() {
    return bounceAfterScale;
  }

  @Schema(nullable = true, description = "Overrides for email recipients by email type for this request")
  public Optional<Map<SingularityEmailType, List<SingularityEmailDestination>>> getEmailConfigurationOverrides() {
    return emailConfigurationOverrides;
}

  @Schema(nullable = true, description = "If true, do not run healthchecks")
  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @Schema(nullable = true, description = "Do not show the UI hint about evenly distributing intances accross racks when scaling")
  public Optional<Boolean> getHideEvenNumberAcrossRacksHint() { return hideEvenNumberAcrossRacksHint; }

  @Schema(nullable = true, description = "Searching for errors in task logs to include in emails using this regex")
  public Optional<String> getTaskLogErrorRegex() { return taskLogErrorRegex; }

  @Schema(nullable = true, description = "Determines if taskLogErrorRegex is case sensitive")
  public Optional<Boolean> getTaskLogErrorRegexCaseSensitive() { return taskLogErrorRegexCaseSensitive; }

  @Schema(nullable = true, description = "a priority level from 0.0 to 1.0 for all tasks associated with the request")
  public Optional<Double> getTaskPriorityLevel() {
    return taskPriorityLevel;
  }

  @Deprecated
  @Schema(nullable = true, description = "the data center associated with this request")
  public Optional<String> getDataCenter() {
    return dataCenter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityRequest that = (SingularityRequest) o;
    return Objects.equals(id, that.id) &&
        requestType == that.requestType &&
        Objects.equals(owners, that.owners) &&
        Objects.equals(numRetriesOnFailure, that.numRetriesOnFailure) &&
        Objects.equals(schedule, that.schedule) &&
        Objects.equals(quartzSchedule, that.quartzSchedule) &&
        Objects.equals(scheduleType, that.scheduleType) &&
        Objects.equals(scheduleTimeZone, that.scheduleTimeZone) &&
        Objects.equals(killOldNonLongRunningTasksAfterMillis, that.killOldNonLongRunningTasksAfterMillis) &&
        Objects.equals(taskExecutionTimeLimitMillis, that.taskExecutionTimeLimitMillis) &&
        Objects.equals(scheduledExpectedRuntimeMillis, that.scheduledExpectedRuntimeMillis) &&
        Objects.equals(waitAtLeastMillisAfterTaskFinishesForReschedule, that.waitAtLeastMillisAfterTaskFinishesForReschedule) &&
        Objects.equals(instances, that.instances) &&
        Objects.equals(skipHealthchecks, that.skipHealthchecks) &&
        Objects.equals(rackSensitive, that.rackSensitive) &&
        Objects.equals(rackAffinity, that.rackAffinity) &&
        Objects.equals(slavePlacement, that.slavePlacement) &&
        Objects.equals(requiredSlaveAttributes, that.requiredSlaveAttributes) &&
        Objects.equals(allowedSlaveAttributes, that.allowedSlaveAttributes) &&
        Objects.equals(loadBalanced, that.loadBalanced) &&
        Objects.equals(group, that.group) &&
        Objects.equals(requiredRole, that.requiredRole) &&
        Objects.equals(readWriteGroups, that.readWriteGroups) &&
        Objects.equals(readOnlyGroups, that.readOnlyGroups) &&
        Objects.equals(bounceAfterScale, that.bounceAfterScale) &&
        Objects.equals(emailConfigurationOverrides, that.emailConfigurationOverrides) &&
        Objects.equals(hideEvenNumberAcrossRacksHint, that.hideEvenNumberAcrossRacksHint) &&
        Objects.equals(taskLogErrorRegex, that.taskLogErrorRegex) &&
        Objects.equals(taskLogErrorRegexCaseSensitive, that.taskLogErrorRegexCaseSensitive) &&
        Objects.equals(taskPriorityLevel, that.taskPriorityLevel) &&
        Objects.equals(maxTasksPerOffer, that.maxTasksPerOffer) &&
        Objects.equals(allowBounceToSameHost, that.allowBounceToSameHost) &&
        Objects.equals(dataCenter, that.dataCenter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, requestType, owners, numRetriesOnFailure, schedule, quartzSchedule, scheduleType, scheduleTimeZone, killOldNonLongRunningTasksAfterMillis, taskExecutionTimeLimitMillis, scheduledExpectedRuntimeMillis, waitAtLeastMillisAfterTaskFinishesForReschedule, instances, skipHealthchecks, rackSensitive, rackAffinity, slavePlacement, requiredSlaveAttributes, allowedSlaveAttributes, loadBalanced, group, requiredRole, readWriteGroups, readOnlyGroups, bounceAfterScale, emailConfigurationOverrides, hideEvenNumberAcrossRacksHint, taskLogErrorRegex, taskLogErrorRegexCaseSensitive, taskPriorityLevel, maxTasksPerOffer, allowBounceToSameHost, dataCenter);
  }

  @Override
  public String toString() {
    return "SingularityRequest{" +
        "id='" + id + '\'' +
        ", requestType=" + requestType +
        ", owners=" + owners +
        ", numRetriesOnFailure=" + numRetriesOnFailure +
        ", schedule=" + schedule +
        ", quartzSchedule=" + quartzSchedule +
        ", scheduleType=" + scheduleType +
        ", scheduleTimeZone=" + scheduleTimeZone +
        ", killOldNonLongRunningTasksAfterMillis=" + killOldNonLongRunningTasksAfterMillis +
        ", taskExecutionTimeLimitMillis=" + taskExecutionTimeLimitMillis +
        ", scheduledExpectedRuntimeMillis=" + scheduledExpectedRuntimeMillis +
        ", waitAtLeastMillisAfterTaskFinishesForReschedule=" + waitAtLeastMillisAfterTaskFinishesForReschedule +
        ", instances=" + instances +
        ", skipHealthchecks=" + skipHealthchecks +
        ", rackSensitive=" + rackSensitive +
        ", rackAffinity=" + rackAffinity +
        ", slavePlacement=" + slavePlacement +
        ", requiredSlaveAttributes=" + requiredSlaveAttributes +
        ", allowedSlaveAttributes=" + allowedSlaveAttributes +
        ", loadBalanced=" + loadBalanced +
        ", group=" + group +
        ", requiredRole=" + requiredRole +
        ", readWriteGroups=" + readWriteGroups +
        ", readOnlyGroups=" + readOnlyGroups +
        ", bounceAfterScale=" + bounceAfterScale +
        ", emailConfigurationOverrides=" + emailConfigurationOverrides +
        ", hideEvenNumberAcrossRacksHint=" + hideEvenNumberAcrossRacksHint +
        ", taskLogErrorRegex=" + taskLogErrorRegex +
        ", taskLogErrorRegexCaseSensitive=" + taskLogErrorRegexCaseSensitive +
        ", taskPriorityLevel=" + taskPriorityLevel +
        ", maxTasksPerOffer=" + maxTasksPerOffer +
        ", allowBounceToSameHost=" + allowBounceToSameHost +
        ", dataCenter=" + dataCenter +
        '}';
  }
}
