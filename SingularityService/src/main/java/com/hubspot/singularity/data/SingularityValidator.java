package com.hubspot.singularity.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.WebExceptions.checkConflict;
import static com.hubspot.singularity.WebExceptions.checkRateLimited;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.quartz.CronExpression;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityContainerType;
import com.hubspot.mesos.SingularityDockerInfo;
import com.hubspot.mesos.SingularityDockerPortMapping;
import com.hubspot.mesos.SingularityMesosTaskLabel;
import com.hubspot.mesos.SingularityPortMappingType;
import com.hubspot.mesos.SingularityVolume;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.ScheduleType;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityPriorityFreezeParent;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestGroup;
import com.hubspot.singularity.SingularityRunNowRequestBuilder;
import com.hubspot.singularity.SingularityShellCommand;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.api.SingularityPriorityFreeze;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.UIConfiguration;
import com.hubspot.singularity.config.shell.ShellCommandDescriptor;
import com.hubspot.singularity.config.shell.ShellCommandOptionDescriptor;
import com.hubspot.singularity.data.history.DeployHistoryHelper;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;

@Singleton
public class SingularityValidator {
  private static final Joiner JOINER = Joiner.on(" ");
  private static final Pattern DEPLOY_ID_ILLEGAL_PATTERN = Pattern.compile("[^a-zA-Z0-9_.]");
  private static final Pattern REQUEST_ID_ILLEGAL_PATTERN = Pattern.compile("[^a-zA-Z0-9_-]");
  private static final Pattern DAY_RANGE_REGEXP = Pattern.compile("[0-7]-[0-7]");
  private static final Pattern COMMA_DAYS_REGEXP = Pattern.compile("([0-7],)+([0-7])?");
  private static final int MAX_STARRED_REQUESTS = 5000;

  private final int maxDeployIdSize;
  private final int maxRequestIdSize;
  private final int maxUserIdSize;
  private final int maxCpusPerRequest;
  private final int maxCpusPerInstance;
  private final int maxInstancesPerRequest;
  private final int defaultBounceExpirationMinutes;
  private final int maxMemoryMbPerRequest;
  private final int maxMemoryMbPerInstance;
  private final int maxDiskMbPerRequest;
  private final int maxDiskMbPerInstance;
  private final Optional<Integer> maxTotalHealthcheckTimeoutSeconds;
  private final long defaultKillHealthcheckAfterSeconds;
  private final int defaultHealthcheckIntervalSeconds;
  private final int defaultHealthcheckStartupTimeoutSeconds;
  private final int defaultHealthcehckMaxRetries;
  private final int defaultHealthcheckResponseTimeoutSeconds;
  private final int maxRunNowTaskLaunchDelay;
  private final int maxDecommissioningSlaves;
  private final boolean spreadAllSlavesEnabled;
  private final boolean allowRequestsWithoutOwners;
  private final boolean createDeployIds;
  private final int deployIdLength;
  private final boolean allowBounceToSameHost;
  private final UIConfiguration uiConfiguration;
  private final SlavePlacement defaultSlavePlacement;
  private final DeployHistoryHelper deployHistoryHelper;
  private final Resources defaultResources;
  private final PriorityManager priorityManager;
  private final DisasterManager disasterManager;
  private final SlaveManager slaveManager;

  @Inject
  public SingularityValidator(SingularityConfiguration configuration, DeployHistoryHelper deployHistoryHelper, PriorityManager priorityManager, DisasterManager disasterManager, SlaveManager slaveManager, UIConfiguration uiConfiguration) {
    this.maxDeployIdSize = configuration.getMaxDeployIdSize();
    this.maxRequestIdSize = configuration.getMaxRequestIdSize();
    this.maxUserIdSize = configuration.getMaxUserIdSize();
    this.allowRequestsWithoutOwners = configuration.isAllowRequestsWithoutOwners();
    this.createDeployIds = configuration.isCreateDeployIds();
    this.deployIdLength = configuration.getDeployIdLength();
    this.deployHistoryHelper = deployHistoryHelper;
    this.priorityManager = priorityManager;

    int defaultCpus = configuration.getMesosConfiguration().getDefaultCpus();
    int defaultMemoryMb = configuration.getMesosConfiguration().getDefaultMemory();
    int defaultDiskMb = configuration.getMesosConfiguration().getDefaultDisk();
    this.defaultBounceExpirationMinutes = configuration.getDefaultBounceExpirationMinutes();
    this.defaultSlavePlacement = configuration.getDefaultSlavePlacement();

    defaultResources = new Resources(defaultCpus, defaultMemoryMb, 0, defaultDiskMb);

    this.maxCpusPerInstance = configuration.getMesosConfiguration().getMaxNumCpusPerInstance();
    this.maxCpusPerRequest = configuration.getMesosConfiguration().getMaxNumCpusPerRequest();
    this.maxMemoryMbPerInstance = configuration.getMesosConfiguration().getMaxMemoryMbPerInstance();
    this.maxMemoryMbPerRequest = configuration.getMesosConfiguration().getMaxMemoryMbPerRequest();
    this.maxDiskMbPerInstance = configuration.getMesosConfiguration().getMaxDiskMbPerInstance();
    this.maxDiskMbPerRequest = configuration.getMesosConfiguration().getMaxDiskMbPerRequest();
    this.maxInstancesPerRequest = configuration.getMesosConfiguration().getMaxNumInstancesPerRequest();

    this.allowBounceToSameHost = configuration.isAllowBounceToSameHost();

    this.maxTotalHealthcheckTimeoutSeconds = configuration.getHealthcheckMaxTotalTimeoutSeconds();
    this.defaultKillHealthcheckAfterSeconds = configuration.getKillTaskIfNotHealthyAfterSeconds();
    this.defaultHealthcheckIntervalSeconds = configuration.getHealthcheckIntervalSeconds();
    this.defaultHealthcheckStartupTimeoutSeconds = configuration.getStartupTimeoutSeconds();
    this.defaultHealthcehckMaxRetries = configuration.getHealthcheckMaxRetries().or(0);
    this.defaultHealthcheckResponseTimeoutSeconds = configuration.getHealthcheckTimeoutSeconds();
    this.maxRunNowTaskLaunchDelay = configuration.getMaxRunNowTaskLaunchDelayDays();

    this.maxDecommissioningSlaves = configuration.getMaxDecommissioningSlaves();
    this.spreadAllSlavesEnabled = configuration.isSpreadAllSlavesEnabled();

    this.uiConfiguration = uiConfiguration;

    this.disasterManager = disasterManager;
    this.slaveManager = slaveManager;
  }

  public SingularityRequest checkSingularityRequest(SingularityRequest request, Optional<SingularityRequest> existingRequest, Optional<SingularityDeploy> activeDeploy,
                                                    Optional<SingularityDeploy> pendingDeploy) {

    checkBadRequest(request.getId() != null && ! REQUEST_ID_ILLEGAL_PATTERN.matcher(request.getId()).find(), "Id cannot be null or contain characters other than [a-zA-Z0-9_]");
    checkBadRequest(request.getRequestType() != null, "RequestType cannot be null or missing");

    if (request.getOwners().isPresent()) {
      checkBadRequest(!request.getOwners().get().contains(null), "Request owners cannot contain null values");
    }

    if (!allowRequestsWithoutOwners) {
      checkBadRequest(request.getOwners().isPresent() && !request.getOwners().get().isEmpty(), "Request must have owners defined (this can be turned off in Singularity configuration)");
    }

    checkBadRequest(request.getId().length() <= maxRequestIdSize, "Request id must be less %s characters or less, it is %s (%s)", maxRequestIdSize, request.getId().length(), request.getId());
    checkBadRequest(!request.getInstances().isPresent() || request.getInstances().get() > 0, "Instances must be greater than 0");
    checkBadRequest(request.getInstancesSafe() <= maxInstancesPerRequest, "Instances (%s) be greater than %s (maxInstancesPerRequest in mesos configuration)", request.getInstancesSafe(), maxInstancesPerRequest);

    if (request.getTaskPriorityLevel().isPresent()) {
      checkBadRequest(request.getTaskPriorityLevel().get() >= 0 && request.getTaskPriorityLevel().get() <= 1, "Request taskPriorityLevel %s is invalid, must be between 0 and 1 (inclusive).", request.getTaskPriorityLevel().get());
    }

    if (existingRequest.isPresent()) {
      checkForIllegalChanges(request, existingRequest.get());
    }

    if (activeDeploy.isPresent()) {
      checkForIllegalResources(request, activeDeploy.get());
    }

    if (pendingDeploy.isPresent()) {
      checkForIllegalResources(request, pendingDeploy.get());
    }

    String quartzSchedule = null;

    if (request.isScheduled()) {
      checkBadRequest(request.getQuartzSchedule().isPresent() || request.getSchedule().isPresent(), "Specify at least one of schedule or quartzSchedule");

      String originalSchedule = request.getQuartzScheduleSafe();

      if (request.getScheduleType().or(ScheduleType.QUARTZ) != ScheduleType.RFC5545) {
        if (request.getQuartzSchedule().isPresent() && !request.getSchedule().isPresent()) {
          checkBadRequest(request.getScheduleType().or(ScheduleType.QUARTZ) == ScheduleType.QUARTZ, "If using quartzSchedule specify scheduleType QUARTZ or leave it blank");
        }

        if (request.getQuartzSchedule().isPresent() || (request.getScheduleType().isPresent() && request.getScheduleType().get() == ScheduleType.QUARTZ)) {
          quartzSchedule = originalSchedule;
        } else {
          checkBadRequest(request.getScheduleType().or(ScheduleType.CRON) == ScheduleType.CRON, "If not using quartzSchedule specify scheduleType CRON or leave it blank");
          checkBadRequest(!request.getQuartzSchedule().isPresent(), "If using schedule type CRON do not specify quartzSchedule");

          quartzSchedule = getQuartzScheduleFromCronSchedule(originalSchedule);
        }

        checkBadRequest(isValidCronSchedule(quartzSchedule), "Schedule %s (from: %s) is not valid", quartzSchedule, originalSchedule);
      } else {
        checkForValidRFC5545Schedule(request.getSchedule().get());
      }
    } else {
      checkBadRequest(!request.getQuartzSchedule().isPresent() && !request.getSchedule().isPresent(), "Non-scheduled requests can not specify a schedule");
      checkBadRequest(!request.getScheduleType().isPresent(), "ScheduleType can only be set for scheduled requests");
    }

    if (request.getScheduleTimeZone().isPresent()) {
      if (!ArrayUtils.contains(TimeZone.getAvailableIDs(), request.getScheduleTimeZone().get())) {
        badRequest("scheduleTimeZone %s does not map to a valid Java TimeZone object (e.g. 'US/Eastern' or 'GMT')", request.getScheduleTimeZone().get());
      }
    }

    if (!request.isLongRunning()) {
      checkBadRequest(!request.isLoadBalanced(), "non-longRunning (scheduled/oneoff) requests can not be load balanced");
      checkBadRequest(!request.isRackSensitive(), "non-longRunning (scheduled/oneoff) requests can not be rack sensitive");
    } else {
      checkBadRequest(!request.getNumRetriesOnFailure().isPresent(), "longRunning requests can not define a NumRetriesOnFailure value");
      checkBadRequest(!request.getKillOldNonLongRunningTasksAfterMillis().isPresent(), "longRunning requests can not define a killOldNonLongRunningTasksAfterMillis value");
      checkBadRequest(!request.getTaskExecutionTimeLimitMillis().isPresent(), "longRunning requests can not define a taskExecutionTimeLimitMillis value");
    }

    if (request.isScheduled()) {
      checkBadRequest(request.getInstances().or(1) == 1, "Scheduler requests can not be ran on more than one instance");
    }

    if (request.getMaxTasksPerOffer().isPresent()) {
      checkBadRequest(request.getMaxTasksPerOffer().get() > 0, "maxTasksPerOffer must be positive");
    }

    return request.toBuilder().setQuartzSchedule(Optional.fromNullable(quartzSchedule)).build();
  }

  public SingularityWebhook checkSingularityWebhook(SingularityWebhook webhook) {
    checkNotNull(webhook, "Webhook is null");
    checkNotNull(webhook.getUri(), "URI is null");

    try {
      new URI(webhook.getUri());
    } catch (URISyntaxException e) {
      badRequest("Invalid URI provided");
    }

    return webhook;
  }

  public SingularityDeploy checkDeploy(SingularityRequest request,
                                       SingularityDeploy deploy,
                                       List<SingularityTaskId> activeTasks,
                                       List<SingularityPendingTaskId> pendingTasks) {
    checkNotNull(request, "request is null");
    checkNotNull(deploy, "deploy is null");

    String deployId = deploy.getId();

    if (deployId == null) {
      checkBadRequest(createDeployIds, "Id must not be null");
      SingularityDeployBuilder builder = deploy.toBuilder();
      builder.setId(createUniqueDeployId());
      deploy = builder.build();
      deployId = deploy.getId();
    }

    checkBadRequest(deployId != null && ! DEPLOY_ID_ILLEGAL_PATTERN.matcher(deployId).find(), "Id cannot be null or contain characters other than [a-zA-Z0-9_.]");
    checkBadRequest(deployId.length() <= maxDeployIdSize, "Deploy id must be %s characters or less, it is %s (%s)", maxDeployIdSize, deployId.length(), deployId);
    checkBadRequest(deploy.getRequestId() != null && deploy.getRequestId().equals(request.getId()), "Deploy id must match request id");

    if (request.isLoadBalanced()) {
      checkBadRequest(deploy.getServiceBasePath().isPresent(), "Deploy for loadBalanced request must include serviceBasePath");
      checkBadRequest(deploy.getLoadBalancerGroups().isPresent() && !deploy.getLoadBalancerGroups().get().isEmpty(), "Deploy for a loadBalanced request must include at least one load balacner group");
    }

    checkForIllegalResources(request, deploy);

    if (deploy.getResources().isPresent()) {
      if (deploy.getHealthcheck().isPresent()) {
        HealthcheckOptions healthcheck = deploy.getHealthcheck().get();
        checkBadRequest(!(healthcheck.getPortIndex().isPresent() && healthcheck.getPortNumber().isPresent()),
          "Can only specify one of portIndex or portNumber for healthchecks");
        if (healthcheck.getPortIndex().isPresent()) {
          checkBadRequest(healthcheck.getPortIndex().get() >= 0, "healthcheckPortIndex cannot be negative");
          checkBadRequest(deploy.getResources().get().getNumPorts() > healthcheck.getPortIndex().get(), String
            .format("Must request %s ports for healthcheckPortIndex %s, only requested %s", healthcheck.getPortIndex().get() + 1, healthcheck.getPortIndex().get(),
              deploy.getResources().get().getNumPorts()));
        }
      }
      if (deploy.getLoadBalancerPortIndex().isPresent()) {
        checkBadRequest(deploy.getLoadBalancerPortIndex().get() >= 0, "loadBalancerPortIndex must be greater than 0");
        checkBadRequest(deploy.getResources().get().getNumPorts() > deploy.getLoadBalancerPortIndex().get(), String
            .format("Must request %s ports for loadBalancerPortIndex %s, only requested %s", deploy.getLoadBalancerPortIndex().get() + 1, deploy.getLoadBalancerPortIndex().get(),
                deploy.getResources().get().getNumPorts()));
      }
    }

    if (deploy.getHealthcheck().isPresent() && !Strings.isNullOrEmpty(deploy.getHealthcheck().get().getUri())) {
      if (!deploy.getResources().isPresent() || deploy.getResources().get().getNumPorts() == 0) {
        checkBadRequest(deploy.getHealthcheck().get().getPortNumber().isPresent(),
          "Either an explicit port number, or port resources and port index must be specified to run healthchecks against a uri");
      }
    }

    if (deploy.getHealthcheck().isPresent() && maxTotalHealthcheckTimeoutSeconds.isPresent()) {
      HealthcheckOptions options = deploy.getHealthcheck().get();
      int intervalSeconds = options.getIntervalSeconds().or(defaultHealthcheckIntervalSeconds);
      int httpTimeoutSeconds = options.getResponseTimeoutSeconds().or(defaultHealthcheckResponseTimeoutSeconds);
      int startupTime = options.getStartupTimeoutSeconds().or(defaultHealthcheckStartupTimeoutSeconds);
      int attempts = options.getMaxRetries().or(defaultHealthcehckMaxRetries) + 1;

      int totalHealthCheckTime = startupTime + ((httpTimeoutSeconds + intervalSeconds) * attempts);
      checkBadRequest(totalHealthCheckTime < maxTotalHealthcheckTimeoutSeconds.get(),
        String.format("Max healthcheck time cannot be greater than %s, (was startup timeout: %s, interval: %s, attempts: %s)", maxTotalHealthcheckTimeoutSeconds.get(), startupTime, intervalSeconds, attempts));
    }

    if (deploy.getHealthcheck().isPresent() && deploy.getHealthcheck().get().getStartupDelaySeconds().isPresent()) {
      int startUpDelay = deploy.getHealthcheck().get().getStartupDelaySeconds().get();

      checkBadRequest(startUpDelay < defaultKillHealthcheckAfterSeconds,
          String.format("Health check startup delay time must be less than max health check run time %s (was %s)", defaultKillHealthcheckAfterSeconds, startUpDelay));
    }

    checkBadRequest(deploy.getCommand().isPresent() && !deploy.getExecutorData().isPresent() ||
            deploy.getExecutorData().isPresent() && deploy.getCustomExecutorCmd().isPresent() && !deploy.getCommand().isPresent() ||
            deploy.getContainerInfo().isPresent(),
        "If not using custom executor, specify a command or containerInfo. If using custom executor, specify executorData and customExecutorCmd and no command.");

    checkBadRequest(!deploy.getContainerInfo().isPresent() || deploy.getContainerInfo().get().getType() != null, "Container type must not be null");

    if (deploy.getLabels().isPresent() && deploy.getMesosTaskLabels().isPresent()) {
      List<SingularityMesosTaskLabel> deprecatedLabels = SingularityMesosTaskLabel.labelsFromMap(deploy.getLabels().get());
      checkBadRequest(deprecatedLabels.containsAll(deploy.getMesosLabels().get()) && deploy.getMesosLabels().get().containsAll(deprecatedLabels), "Can only specify one of 'labels' or 'mesosLabels");
    }

    if (deploy.getTaskLabels().isPresent() && deploy.getMesosTaskLabels().isPresent()) {
      for (Map.Entry<Integer, Map<String, String>> entry : deploy.getTaskLabels().get().entrySet()) {
        List<SingularityMesosTaskLabel> deprecatedLabels = SingularityMesosTaskLabel.labelsFromMap(entry.getValue());
        checkBadRequest(deploy.getMesosTaskLabels().get().containsKey(entry.getKey())
          && deprecatedLabels.containsAll(deploy.getMesosTaskLabels().get().get(entry.getKey()))
          && deploy.getMesosTaskLabels().get().get(entry.getKey()).containsAll(deprecatedLabels),
          "Can only specify one of 'taskLabels' or 'mesosTaskLabels");
      }
    }

    if (deploy.getContainerInfo().isPresent()) {
      SingularityContainerInfo containerInfo = deploy.getContainerInfo().get();
      checkBadRequest(containerInfo.getType() != null, "container type may not be null");
      if (containerInfo.getVolumes().isPresent() && !containerInfo.getVolumes().get().isEmpty()) {
        for (SingularityVolume volume : containerInfo.getVolumes().get()) {
          checkBadRequest(volume.getContainerPath() != null, "volume containerPath may not be null");
        }
      }
      if (deploy.getContainerInfo().get().getType() == SingularityContainerType.DOCKER) {
        checkDocker(deploy);
      }
    }

    checkBadRequest(deployHistoryHelper.isDeployIdAvailable(request.getId(), deployId), "Can not deploy a deploy that has already been deployed");

    if (deploy.getRunImmediately().isPresent()) {
      deploy = checkImmediateRunDeploy(request, deploy, deploy.getRunImmediately().get(), activeTasks, pendingTasks);
    }

    if (request.isDeployable()) {
      checkRequestForPriorityFreeze(request);
    }

    return deploy;
  }

  private SingularityDeploy checkImmediateRunDeploy(SingularityRequest request,
                                                    SingularityDeploy deploy,
                                                    SingularityRunNowRequest runNowRequest,
                                                    List<SingularityTaskId> activeTasks,
                                                    List<SingularityPendingTaskId> pendingTasks) {
    if (!request.isScheduled() && !request.isOneOff()) {
      throw badRequest("Can not request an immediate run of a non-scheduled / always running request (%s)", request);
    }

    return deploy.toBuilder()
        .setRunImmediately(Optional.of(fillRunNowRequest(Optional.of(runNowRequest))))
        .build();
  }

  public SingularityPendingRequest checkRunNowRequest(String deployId,
                                                      Optional<String> userEmail,
                                                      SingularityRequest request,
                                                      Optional<SingularityRunNowRequest> maybeRunNowRequest,
                                                      List<SingularityTaskId> activeTasks,
                                                      List<SingularityPendingTaskId> pendingTasks) {
    SingularityRunNowRequest runNowRequest = fillRunNowRequest(maybeRunNowRequest);
    PendingType pendingType;
    if (request.isScheduled()) {
      pendingType = PendingType.IMMEDIATE;
      checkConflict(activeTasks.isEmpty(), "Cannot request immediate run of a scheduled job which is currently running (%s)", activeTasks);
    } else if (request.isOneOff()) {
      pendingType = PendingType.ONEOFF;
      if (request.getInstances().isPresent()) {
        checkRateLimited(
            activeTasks.size() + pendingTasks.size() < request.getInstances().get(),
            "No more than %s tasks allowed to run concurrently for request %s (%s active, %s pending)",
            request.getInstances().get(), request, activeTasks.size(), pendingTasks.size());
      }
    } else {
      throw badRequest("Can not request an immediate run of a non-scheduled / always running request (%s)", request);
    }

    if (runNowRequest.getRunAt().isPresent()
        && runNowRequest.getRunAt().get() > (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(maxRunNowTaskLaunchDelay))) {
      throw badRequest("Task launch delay can be at most %d days from now.", maxRunNowTaskLaunchDelay);
    }

    return new SingularityPendingRequest(
        request.getId(),
        deployId,
        System.currentTimeMillis(),
        userEmail,
        pendingType,
        runNowRequest.getCommandLineArgs(),
        Optional.of(getRunId(runNowRequest.getRunId())),
        runNowRequest.getSkipHealthchecks(),
        runNowRequest.getMessage(),
        Optional.absent(),
        runNowRequest.getResources(),
        runNowRequest.getS3UploaderAdditionalFiles(),
        runNowRequest.getRunAsUserOverride(),
        runNowRequest.getEnvOverrides(),
        runNowRequest.getExtraArtifacts(),
        runNowRequest.getRunAt()
    );
  }

  private SingularityRunNowRequest fillRunNowRequest(Optional<SingularityRunNowRequest> maybeRequest) {
    if (maybeRequest.isPresent()) {
      SingularityRunNowRequest request = maybeRequest.get();
      return new SingularityRunNowRequest(
          request.getMessage(),
          request.getSkipHealthchecks(),
          Optional.of(getRunId(request.getRunId())),
          request.getCommandLineArgs(),
          request.getResources(),
          request.getS3UploaderAdditionalFiles(),
          request.getRunAsUserOverride(),
          request.getEnvOverrides(),
          request.getExtraArtifacts(),
          request.getRunAt());
    } else {
      return new SingularityRunNowRequestBuilder()
          .setRunId(getRunId(Optional.absent()))
          .build();
    }
  }

  private String getRunId(Optional<String> maybeRunId) {
    if (maybeRunId.isPresent()) {
      String runId = maybeRunId.get();
      if (runId.length() > 100) {
        throw badRequest("RunId must be less than 100 characters. RunId %s has %s characters", runId, runId.length());
      } else {
        return runId;
      }
    } else {
      return UUID.randomUUID().toString();
    }
  }

  /**
   *
   * Transforms unix cron into quartz compatible cron;
   *
   * - adds seconds if not included
   * - switches either day of month or day of week to ?
   *
   * Field Name   Allowed Values          Allowed Special Characters
   * Seconds      0-59                    - * /
   * Minutes      0-59                    - * /
   * Hours        0-23                    - * /
   * Day-of-month 1-31                    - * ? / L W
   * Month        1-12 or JAN-DEC         - * /
   * Day-of-Week  1-7 or SUN-SAT          - * ? / L #
   * Year         (Optional), 1970-2199   - * /
   */
  public String getQuartzScheduleFromCronSchedule(String schedule) {
    if (schedule == null) {
      return null;
    }

    String[] split = schedule.split(" ");

    checkBadRequest(split.length >= 5, "Schedule %s is invalid because it contained only %s splits (looking for at least 5)", schedule, split.length);

    List<String> newSchedule = Lists.newArrayListWithCapacity(6);

    boolean hasSeconds = split.length > 5;

    if (!hasSeconds) {
      newSchedule.add("0");
    } else {
      newSchedule.add(split[0]);
    }

    int indexMod = hasSeconds ? 1 : 0;

    newSchedule.add(split[indexMod]);
    newSchedule.add(split[indexMod + 1]);

    String dayOfMonth = split[indexMod + 2];
    String dayOfWeek = split[indexMod + 4];

    if (dayOfWeek.equals("*")) {
      dayOfWeek = "?";
    } else if (!dayOfWeek.equals("?")) {
      dayOfMonth = "?";
    }

    if (isValidInteger(dayOfWeek)) {
      dayOfWeek = getNewDayOfWeekValue(schedule, Integer.parseInt(dayOfWeek));
    } else if (DAY_RANGE_REGEXP.matcher(dayOfWeek).matches() || COMMA_DAYS_REGEXP.matcher(dayOfWeek).matches()) {
      String separator = ",";

      if (DAY_RANGE_REGEXP.matcher(dayOfWeek).matches()) {
        separator = "-";
      }

      final String[] dayOfWeekSplit = dayOfWeek.split(separator);
      final List<String> dayOfWeekValues = new ArrayList<>(dayOfWeekSplit.length);

      for (String dayOfWeekValue : dayOfWeekSplit) {
        dayOfWeekValues.add(getNewDayOfWeekValue(schedule, Integer.parseInt(dayOfWeekValue)));
      }

      dayOfWeek = Joiner.on(separator).join(dayOfWeekValues);
    }

    newSchedule.add(dayOfMonth);
    newSchedule.add(split[indexMod + 3]);
    newSchedule.add(dayOfWeek);

    return JOINER.join(newSchedule);
  }

  private void checkForIllegalChanges(SingularityRequest request, SingularityRequest existingRequest) {
    if (request.getRequestType() != existingRequest.getRequestType()) {
      boolean validWorkerServiceTransition = (existingRequest.getRequestType() == RequestType.SERVICE && !existingRequest.isLoadBalanced() && request.getRequestType() == RequestType.WORKER) ||
          (request.getRequestType() == RequestType.SERVICE && !request.isLoadBalanced() && existingRequest.getRequestType() == RequestType.WORKER);
      checkBadRequest(validWorkerServiceTransition, String.format("Request can not change requestType from %s to %s", existingRequest.getRequestType(), request.getRequestType()));
    }
    checkBadRequest(request.isLoadBalanced() == existingRequest.isLoadBalanced(), "Request can not change whether it is load balanced");
  }

  private void checkForIllegalResources(SingularityRequest request, SingularityDeploy deploy) {
    int instances = request.getInstancesSafe();
    double cpusPerInstance = deploy.getResources().or(defaultResources).getCpus();
    double memoryMbPerInstance = deploy.getResources().or(defaultResources).getMemoryMb();
    double diskMbPerInstance = deploy.getResources().or(defaultResources).getDiskMb();

    checkBadRequest(cpusPerInstance > 0, "Request must have more than 0 cpus");
    checkBadRequest(memoryMbPerInstance > 0, "Request must have more than 0 memoryMb");
    checkBadRequest(diskMbPerInstance >= 0, "Request must have non-negative diskMb");

    checkBadRequest(cpusPerInstance <= maxCpusPerInstance, "Deploy %s uses too many cpus %s (maxCpusPerInstance %s in mesos configuration)", deploy.getId(), cpusPerInstance, maxCpusPerInstance);
    checkBadRequest(cpusPerInstance * instances <= maxCpusPerRequest,
        "Deploy %s uses too many cpus %s (%s*%s) (cpusPerRequest %s in mesos configuration)", deploy.getId(), cpusPerInstance * instances, cpusPerInstance, instances, maxCpusPerRequest);

    checkBadRequest(memoryMbPerInstance <= maxMemoryMbPerInstance,
        "Deploy %s uses too much memoryMb %s (maxMemoryMbPerInstance %s in mesos configuration)", deploy.getId(), memoryMbPerInstance, maxMemoryMbPerInstance);
    checkBadRequest(memoryMbPerInstance * instances <= maxMemoryMbPerRequest, "Deploy %s uses too much memoryMb %s (%s*%s) (maxMemoryMbPerRequest %s in mesos configuration)", deploy.getId(),
        memoryMbPerInstance * instances, memoryMbPerInstance, instances, maxMemoryMbPerRequest);

    checkBadRequest(diskMbPerInstance <= maxDiskMbPerInstance,
        "Deploy %s uses too much diskMb %s (maxDiskMbPerInstance %s in mesos configuration)", deploy.getId(), diskMbPerInstance, maxDiskMbPerInstance);
    checkBadRequest(diskMbPerInstance * instances <= maxDiskMbPerRequest, "Deploy %s uses too much diskMb %s (%s*%s) (maxDiskMbPerRequest %s in mesos configuration)", deploy.getId(),
        diskMbPerInstance * instances, diskMbPerInstance, instances, maxDiskMbPerRequest);
  }

  private void checkForValidRFC5545Schedule(String schedule) {
    try {
      new RecurrenceRule(schedule);
    } catch (InvalidRecurrenceRuleException ex) {
      badRequest("Schedule %s is not a valid RFC5545 schedule, error is: %s", schedule, ex);
    }
  }

  private String createUniqueDeployId() {
    UUID id = UUID.randomUUID();
    String result = Hashing.sha256().newHasher().putLong(id.getLeastSignificantBits()).putLong(id.getMostSignificantBits()).hash().toString();
    return result.substring(0, deployIdLength);
  }

  private void checkDocker(SingularityDeploy deploy) {
    if (deploy.getResources().isPresent() && deploy.getContainerInfo().get().getDocker().isPresent()) {
      final SingularityDockerInfo dockerInfo = deploy.getContainerInfo().get().getDocker().get();
      final int numPorts = deploy.getResources().get().getNumPorts();

      checkBadRequest(dockerInfo.getImage() != null, "docker image may not be null");

      for (SingularityDockerPortMapping portMapping : dockerInfo.getPortMappings()) {
        if (portMapping.getContainerPortType() == SingularityPortMappingType.FROM_OFFER) {
          checkBadRequest(portMapping.getContainerPort() >= 0 && portMapping.getContainerPort() < numPorts,
              "Index of port resource for containerPort must be between 0 and %d (inclusive)", numPorts - 1);
        }

        if (portMapping.getHostPortType() == SingularityPortMappingType.FROM_OFFER) {
          checkBadRequest(portMapping.getHostPort() >= 0 && portMapping.getHostPort() < numPorts,
              "Index of port resource for hostPort must be between 0 and %d (inclusive)", numPorts - 1);
        }
      }
    }
  }

  private boolean isValidCronSchedule(String schedule) {
    return CronExpression.isValidExpression(schedule);
  }

  /**
   * Standard cron: day of week (0 - 6) (0 to 6 are Sunday to Saturday, or use names; 7 is Sunday, the same as 0)
   * Quartz: 1-7 or SUN-SAT
   */
  private String getNewDayOfWeekValue(String schedule, int dayOfWeekValue) {
    String newDayOfWeekValue = null;

    checkBadRequest(dayOfWeekValue >= 0 && dayOfWeekValue <= 7, "Schedule %s is invalid, day of week (%s) is not 0-7", schedule, dayOfWeekValue);

    switch (dayOfWeekValue) {
      case 7:
      case 0:
        newDayOfWeekValue = "SUN";
        break;
      case 1:
        newDayOfWeekValue = "MON";
        break;
      case 2:
        newDayOfWeekValue = "TUE";
        break;
      case 3:
        newDayOfWeekValue = "WED";
        break;
      case 4:
        newDayOfWeekValue = "THU";
        break;
      case 5:
        newDayOfWeekValue = "FRI";
        break;
      case 6:
        newDayOfWeekValue = "SAT";
        break;
      default:
        badRequest("Schedule %s is invalid, day of week (%s) is not 0-7", schedule, dayOfWeekValue);
        break;
    }

    return newDayOfWeekValue;
  }

  public void checkResourcesForBounce(SingularityRequest request, boolean isIncremental) {
    SlavePlacement placement = request.getSlavePlacement().or(defaultSlavePlacement);

    if ((isAllowBounceToSameHost(request) && placement == SlavePlacement.SEPARATE_BY_REQUEST)
      || (!isAllowBounceToSameHost(request) && placement != SlavePlacement.GREEDY && placement != SlavePlacement.OPTIMISTIC)) {
      int currentActiveSlaveCount = slaveManager.getNumObjectsAtState(MachineState.ACTIVE);
      int requiredSlaveCount = isIncremental ? request.getInstancesSafe() + 1 : request.getInstancesSafe() * 2;

      checkBadRequest(currentActiveSlaveCount >= requiredSlaveCount, "Not enough active slaves to successfully scale request %s to %s instances (minimum required: %s, current: %s).", request.getId(), request.getInstancesSafe(), requiredSlaveCount, currentActiveSlaveCount);
    }
  }

  private boolean isAllowBounceToSameHost(SingularityRequest request) {
    if (request.getAllowBounceToSameHost().isPresent()) {
      return request.getAllowBounceToSameHost().get();
    } else {
      return allowBounceToSameHost;
    }
  }

  public void checkScale(SingularityRequest request, Optional<Integer> previousScale) {
    SlavePlacement placement = request.getSlavePlacement().or(defaultSlavePlacement);

    if (placement != SlavePlacement.GREEDY && placement != SlavePlacement.OPTIMISTIC) {
      int currentActiveSlaveCount = slaveManager.getNumObjectsAtState(MachineState.ACTIVE);
      int requiredSlaveCount = request.getInstancesSafe();

      if (previousScale.isPresent() && placement == SlavePlacement.SEPARATE_BY_REQUEST) {
        requiredSlaveCount += previousScale.get();
      }

      checkBadRequest(currentActiveSlaveCount >= requiredSlaveCount, "Not enough active slaves to successfully complete a bounce of request %s (minimum required: %s, current: %s). Consider deploying, or changing the slave placement strategy instead.", request.getId(), requiredSlaveCount, currentActiveSlaveCount);
    }
  }

  public void validateExpiringMachineStateChange(Optional<SingularityMachineChangeRequest> maybeChangeRequest, MachineState currentState, Optional<SingularityExpiringMachineState> currentExpiringObject) {
    if (!maybeChangeRequest.isPresent() || !maybeChangeRequest.get().getDurationMillis().isPresent()) {
      return;
    }

    SingularityMachineChangeRequest changeRequest = maybeChangeRequest.get();

    checkBadRequest(changeRequest.getRevertToState().isPresent(), "Must include a machine state to revert to for an expiring machine state change");
    MachineState newState = changeRequest.getRevertToState().get();

    checkConflict(!currentExpiringObject.isPresent(), "A current expiring object already exists, delete it first");
    checkBadRequest(!(newState == MachineState.STARTING_DECOMMISSION && currentState.isDecommissioning()), "Cannot start decommission when it has already been started");
    checkBadRequest(!(((newState == MachineState.DECOMMISSIONING) || (newState == MachineState.DECOMMISSIONED)) && (currentState == MachineState.FROZEN)), "Cannot transition from FROZEN to DECOMMISSIONING or DECOMMISSIONED");
    checkBadRequest(!(((newState == MachineState.DECOMMISSIONING) || (newState == MachineState.DECOMMISSIONED)) && (currentState == MachineState.ACTIVE)), "Cannot transition from ACTIVE to DECOMMISSIONING or DECOMMISSIONED");
    checkBadRequest(!(newState == MachineState.FROZEN && currentState.isDecommissioning()), "Cannot transition from a decommissioning state to FROZEN");

    List<MachineState> systemOnlyStateTransitions = ImmutableList.of(MachineState.DEAD, MachineState.MISSING_ON_STARTUP, MachineState.DECOMMISSIONING);
    checkBadRequest(!systemOnlyStateTransitions.contains(newState), "States {} are reserved for system usage, you cannot manually transition to {}", systemOnlyStateTransitions, newState);

    checkBadRequest(!(newState == MachineState.DECOMMISSIONED && !changeRequest.isKillTasksOnDecommissionTimeout()), "Must specify that all tasks on slave get killed if transitioning to DECOMMISSIONED state");
  }

  public void validateDecommissioningCount() {
    int decommissioning = slaveManager.getObjectsFiltered(MachineState.DECOMMISSIONING).size() + slaveManager.getObjectsFiltered(MachineState.STARTING_DECOMMISSION).size();
    checkBadRequest(decommissioning < maxDecommissioningSlaves,
        "%s slaves are already decommissioning state (%s allowed at once). Allow these slaves to finish before decommissioning another", decommissioning, maxDecommissioningSlaves);
  }

  public void checkActionEnabled(SingularityAction action) {
    checkConflict(!disasterManager.isDisabled(action), disasterManager.getDisabledAction(action).getMessage());
  }

  private boolean isValidInteger(String strValue) {
    try {
      Integer.parseInt(strValue);
      return true;
    } catch (NumberFormatException nfe) {
      return false;
    }
  }

  public boolean isSpreadAllSlavesEnabled() {
    return spreadAllSlavesEnabled;
  }

  public void checkUserId(String userId) {
    checkBadRequest(!Strings.isNullOrEmpty(userId), "User ID must be present and non-null");
    checkBadRequest(!(userId.length() > maxUserIdSize), "User ID cannot be more than %s characters, it was %s", maxUserIdSize, userId.length());
  }

  public void checkStarredRequests(Set<String> starredRequests) {
    checkBadRequest(!(starredRequests.size() > MAX_STARRED_REQUESTS), "Cannot have more than %s starred requests", MAX_STARRED_REQUESTS);
  }

  public SingularityPriorityFreeze checkSingularityPriorityFreeze(SingularityPriorityFreeze priorityFreeze) {
    checkBadRequest(priorityFreeze.getMinimumPriorityLevel() > 0 && priorityFreeze.getMinimumPriorityLevel() <= 1, "minimumPriorityLevel %s is invalid, must be greater than 0 and less than or equal to 1.", priorityFreeze.getMinimumPriorityLevel());

    // auto-generate actionId if not set
    if (!priorityFreeze.getActionId().isPresent()) {
      priorityFreeze = new SingularityPriorityFreeze(priorityFreeze.getMinimumPriorityLevel(), priorityFreeze.isKillTasks(), priorityFreeze.getMessage(), Optional.of(UUID.randomUUID().toString()));
    }

    return priorityFreeze;
  }

  public void checkRequestForPriorityFreeze(SingularityRequest request) {
    final Optional<SingularityPriorityFreezeParent> maybePriorityFreeze = priorityManager.getActivePriorityFreeze();

    if (!maybePriorityFreeze.isPresent()) {
      return;
    }

    final double taskPriorityLevel = priorityManager.getTaskPriorityLevelForRequest(request);

    checkBadRequest(taskPriorityLevel >= maybePriorityFreeze.get().getPriorityFreeze().getMinimumPriorityLevel(), "Priority level of request %s (%s) is lower than active priority freeze (%s)",
      request.getId(), taskPriorityLevel, maybePriorityFreeze.get().getPriorityFreeze().getMinimumPriorityLevel());
  }

  public SingularityBounceRequest checkBounceRequest(SingularityBounceRequest defaultBounceRequest) {
    if (defaultBounceRequest.getDurationMillis().isPresent()) {
      return defaultBounceRequest;
    }
    final long durationMillis = TimeUnit.MINUTES.toMillis(defaultBounceExpirationMinutes);
    return defaultBounceRequest
        .toBuilder()
        .setDurationMillis(Optional.of(durationMillis))
        .build();
  }

  public void checkRequestGroup(SingularityRequestGroup requestGroup) {
    checkBadRequest(requestGroup.getId() != null && ! REQUEST_ID_ILLEGAL_PATTERN.matcher(requestGroup.getId()).find(), "Id cannot be null or contain characters other than [a-zA-Z0-9_-]");
    checkBadRequest(requestGroup.getId().length() < maxRequestIdSize, "Id must be less than %s characters, it is %s (%s)", maxRequestIdSize, requestGroup.getId().length(), requestGroup.getId());

    checkBadRequest(requestGroup.getRequestIds() != null, "requestIds cannot be null");
  }

  public void checkValidShellCommand(final SingularityShellCommand shellCommand) {
    Optional<ShellCommandDescriptor> commandDescriptor = Iterables.tryFind(uiConfiguration.getShellCommands(), new Predicate<ShellCommandDescriptor>() {
      @Override
      public boolean apply(ShellCommandDescriptor input) {
        return input.getName().equals(shellCommand.getName());
      }
    });

    if (!commandDescriptor.isPresent()) {
      throw WebExceptions.badRequest("Shell command %s not in %s", shellCommand.getName(), uiConfiguration.getShellCommands());
    }

    Set<String> options = Sets.newHashSetWithExpectedSize(commandDescriptor.get().getOptions().size());
    for (ShellCommandOptionDescriptor option : commandDescriptor.get().getOptions()) {
      options.add(option.getName());
    }

    if (shellCommand.getOptions().isPresent()) {
      for (String option : shellCommand.getOptions().get()) {
        if (!options.contains(option)) {
          throw WebExceptions.badRequest("Shell command %s does not have option %s (%s)", shellCommand.getName(), option, options);
        }
      }
    }
  }
}
