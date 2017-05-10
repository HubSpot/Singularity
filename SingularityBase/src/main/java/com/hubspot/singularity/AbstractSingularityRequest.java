package com.hubspot.singularity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Lazy;
import org.immutables.value.Value.Style.ImplementationVisibility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@Value.Style(
    get = {"is*", "get*"}, // Detect 'get' and 'is' prefixes in accessor methods
    init = "set*", // Builder initialization methods will have 'set' prefix
    typeAbstract = {"Abstract*", "*IF"}, // 'Abstract' prefix, and 'IF' suffix, will be detected and trimmed
    typeImmutable = "*", // No prefix or suffix for generated immutable type
    optionalAcceptNullable = true, // allow for an Optional<T> to have a setter that takes a null value of T
    visibility = ImplementationVisibility.SAME, // Generated class will have the same visibility as the abstract class/interface)
    jdkOnly = true,  // For Guava 18+, this stops MoreObjects from being used in toString and ImmutableHashMap.Builder from being used for building map fields (among other effects).
    allParameters = true)
public abstract class AbstractSingularityRequest {

  @ApiModelProperty(required=true, value="A unique id for the request")
  public abstract String getId();

  @ApiModelProperty(required=false, value="A list of emails for the owners of this request")
  public abstract Optional<List<String>> getOwners();

  @ApiModelProperty(required=false, value="For scheduled jobs, retry up to this many times if the job fails")
  public abstract Optional<Integer> getNumRetriesOnFailure();

  @ApiModelProperty(required=false, value="A schedule in cron, RFC5545, or quartz format")
  public abstract Optional<String> getSchedule();

  @ApiModelProperty(required=false, value="A schedule in quartz format")
  public abstract Optional<String> getQuartzSchedule();

  @ApiModelProperty(required=false, value="Time zone to use when running the")
  public abstract Optional<String> getScheduleTimeZone();

  @ApiModelProperty(required=false, value="A count of tasks to run for long-running requests")
  public abstract Optional<Integer> getInstances();

  @ApiModelProperty(required=false, value="Spread instances for this request evenly across separate racks")
  @Default
  public boolean isRackSensitive() {
    return false;
  }

  @ApiModelProperty(required=false, value="Indicates that a SERVICE should be load balanced")
  @Default
  public boolean isLoadBalanced() {
    return false;
  }

  @ApiModelProperty(required=true, value="The type of request, can be SERVICE, WORKER, SCHEDULED, ON_DEMAND, or RUN_ONCE")
  @Nullable
  public abstract RequestType getRequestType();

  @Check
  public AbstractSingularityRequest validate() {
    if (getRequestType() == null) {
      return SingularityRequest.builder().from(this)
          .setRequestType(RequestType.fromDaemonAndScheduleAndLoadBalanced(getSchedule(), getDaemon(), isLoadBalanced()))
          .build();
    } else {
      return this;
    }
  }

  @Deprecated
  public abstract Optional<Boolean> getDaemon();

  @ApiModelProperty(required=false, value="For non-long-running request types, kill a task after this amount of time if it has been put into CLEANING and has not shut down")
  public abstract Optional<Long> getKillOldNonLongRunningTasksAfterMillis();

  @ApiModelProperty(required=false, value="If set, don't allow any taks for this request to run for longer than this amount of time")
  public abstract Optional<Long> getTaskExecutionTimeLimitMillis();

  @ApiModelProperty(required=false, value="The type of schedule associated with the scheduled field. Can be CRON, QUARTZ, or RFC5545")
  public abstract Optional<ScheduleType> getScheduleType();

  @ApiModelProperty(required=false, value="If set, prefer this specific rack when launching tasks")
  public abstract Optional<List<String>> getRackAffinity();

  @ApiModelProperty(required=false, value="Strategy for determining where to place new tasks. Can be SEPARATE, OPTIMISTIC, GREEDY, SEPARATE_BY_DEPLOY, or SEPARATE_BY_REQUEST")
  public abstract Optional<SlavePlacement> getSlavePlacement();

  @ApiModelProperty(required=false, value="Expected time for a non-long-running task to run. Singularity will notify owners if a task exceeds this time")
  public abstract Optional<Long> getScheduledExpectedRuntimeMillis();

  @ApiModelProperty(required=false, value="Only allow tasks for this request to run on slaves which have these attributes")
  public abstract Optional<Map<String, String>> getRequiredSlaveAttributes();

  @ApiModelProperty(required=false, value="Allow tasks to run on slaves with these attributes, but do not restrict them to only these slaves")
  public abstract Optional<Map<String, String>> getAllowedSlaveAttributes();

  @ApiModelProperty(required=false, value="Do not schedule more than this many tasks using a single offer from a single mesos slave")
  public abstract Optional<Integer> getMaxTasksPerOffer();

  @ApiModelProperty(required=false, value="If set to true, allow tasks to be scheduled on the same host as an existing active task when bouncing")
  public abstract Optional<Boolean> getAllowBounceToSameHost();

  @JsonIgnore
  @Auxiliary
  public int getInstancesSafe() {
    return getInstances().or(1);
  }

  @JsonIgnore
  @Auxiliary
  public boolean isScheduled() {
    return getRequestType() == RequestType.SCHEDULED;
  }

  @JsonIgnore
  @Auxiliary
  public String getQuartzScheduleSafe() {
    if (getQuartzSchedule().isPresent()) {
      return getQuartzSchedule().get();
    }

    return getSchedule().get();
  }

  @JsonIgnore
  @Lazy
  public boolean isLongRunning() {
    return getRequestType().isLongRunning();
  }

  @JsonIgnore
  @Lazy
  public boolean isAlwaysRunning() {
    return getRequestType().isAlwaysRunning();
  }

  @JsonIgnore
  @Lazy
  public boolean isOneOff() {
    return getRequestType() == RequestType.ON_DEMAND;
  }

  @JsonIgnore
  @Lazy
  public boolean isDeployable() {
    return getRequestType().isDeployable();
  }

  @JsonIgnore
  @Auxiliary
  public ScheduleType getScheduleTypeSafe() {
    return getScheduleType().or(ScheduleType.CRON);
  }

  @ApiModelProperty(required=false, value="When a scheduled job finishes, wait at least this long before rescheduling it")
  public abstract Optional<Long> getWaitAtLeastMillisAfterTaskFinishesForReschedule();

  @ApiModelProperty(required=false, value="Auth group associated with this request. Users in this group are allowed read/write access to this request")
  public abstract Optional<String> getGroup();

  @ApiModelProperty(required=false, value="Mesos Role required for this request. Only offers with the required role will be accepted to execute the tasks associated with the request")
  public abstract Optional<String> getRequiredRole();

  @ApiModelProperty(required=false, value="Users in these groups are allowed read/write access to this request")
  public abstract Optional<Set<String>> getReadWriteGroups();

  @ApiModelProperty(required=false, value="Users in these groups are allowed read only access to this request")
  public abstract Optional<Set<String>> getReadOnlyGroups();

  @ApiModelProperty(required=false, value="Used for SingularityUI. If true, automatically trigger a bounce after changing the request's instance count")
  public abstract Optional<Boolean> getBounceAfterScale();

  @ApiModelProperty(required=false, value="Overrides for email recipients by email type for this request")
  public abstract Optional<Map<SingularityEmailType, List<SingularityEmailDestination>>> getEmailConfigurationOverrides();

  @ApiModelProperty(required=false, value="If true, do not run healthchecks")
  public abstract Optional<Boolean> getSkipHealthchecks();

  public abstract Optional<Boolean> getHideEvenNumberAcrossRacksHint();

  @ApiModelProperty(required=false, value="Searching for errors in task logs to include in emails using this regex")
  public abstract Optional<String> getTaskLogErrorRegex();

  @ApiModelProperty(required=false, value="Determines if taskLogErrorRegex is case sensitive")
  public abstract Optional<Boolean> getTaskLogErrorRegexCaseSensitive();

  @ApiModelProperty(required=false, value="a priority level from 0.0 to 1.0 for all tasks associated with the request")
  public abstract Optional<Double> getTaskPriorityLevel();
}
