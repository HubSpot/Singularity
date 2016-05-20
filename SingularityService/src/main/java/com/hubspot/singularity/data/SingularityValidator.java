package com.hubspot.singularity.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hubspot.singularity.WebExceptions.checkBadRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import org.quartz.CronExpression;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityContainerType;
import com.hubspot.mesos.SingularityDockerInfo;
import com.hubspot.mesos.SingularityDockerNetworkType;
import com.hubspot.mesos.SingularityDockerPortMapping;
import com.hubspot.mesos.SingularityPortMappingType;
import com.hubspot.mesos.SingularityVolume;
import com.hubspot.singularity.ScheduleType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.api.SingularityPriorityKillRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.DeployHistoryHelper;

@Singleton
public class SingularityValidator {
  private static final Joiner JOINER = Joiner.on(" ");
  private static final List<Character> DEPLOY_ID_ILLEGAL_CHARACTERS = Arrays.asList('@', '-', '\\', '/', '*', '?', '%', ' ', '[', ']', '#', '$'); // Characters that make Mesos or URL bars sad
  private static final List<Character> REQUEST_ID_ILLEGAL_CHARACTERS = Arrays.asList('@', '\\', '/', '*', '?', '%', ' ', '[', ']', '#', '$'); // Characters that make Mesos or URL bars sad

  private final int maxDeployIdSize;
  private final int maxRequestIdSize;
  private final int maxCpusPerRequest;
  private final int maxCpusPerInstance;
  private final int maxInstancesPerRequest;
  private final int maxMemoryMbPerRequest;
  private final int defaultCpus;
  private final int defaultMemoryMb;
  private final int defaultDiskMb;
  private final int maxMemoryMbPerInstance;
  private final boolean allowRequestsWithoutOwners;
  private final boolean createDeployIds;
  private final int deployIdLength;
  private final DeployHistoryHelper deployHistoryHelper;
  private final Resources defaultResources;

  @Inject
  public SingularityValidator(SingularityConfiguration configuration, DeployHistoryHelper deployHistoryHelper, RequestManager requestManager) {
    this.maxDeployIdSize = configuration.getMaxDeployIdSize();
    this.maxRequestIdSize = configuration.getMaxRequestIdSize();
    this.allowRequestsWithoutOwners = configuration.isAllowRequestsWithoutOwners();
    this.createDeployIds = configuration.isCreateDeployIds();
    this.deployIdLength = configuration.getDeployIdLength();
    this.deployHistoryHelper = deployHistoryHelper;

    this.defaultCpus = configuration.getMesosConfiguration().getDefaultCpus();
    this.defaultMemoryMb = configuration.getMesosConfiguration().getDefaultMemory();
    this.defaultDiskMb = configuration.getMesosConfiguration().getDefaultDisk();

    defaultResources = new Resources(defaultCpus, defaultMemoryMb, 0, defaultDiskMb);

    this.maxCpusPerInstance = configuration.getMesosConfiguration().getMaxNumCpusPerInstance();
    this.maxCpusPerRequest = configuration.getMesosConfiguration().getMaxNumCpusPerRequest();
    this.maxMemoryMbPerInstance = configuration.getMesosConfiguration().getMaxMemoryMbPerInstance();
    this.maxMemoryMbPerRequest = configuration.getMesosConfiguration().getMaxMemoryMbPerRequest();
    this.maxInstancesPerRequest = configuration.getMesosConfiguration().getMaxNumInstancesPerRequest();
  }

  private void checkForIllegalChanges(SingularityRequest request, SingularityRequest existingRequest) {
    checkBadRequest(request.getRequestType() == existingRequest.getRequestType(), String.format("Request can not change requestType from %s to %s", existingRequest.getRequestType(), request.getRequestType()));
    checkBadRequest(request.isLoadBalanced() == existingRequest.isLoadBalanced(), "Request can not change whether it is load balanced");
  }

  private void checkForIllegalResources(SingularityRequest request, SingularityDeploy deploy) {
    int instances = request.getInstancesSafe();
    double cpusPerInstance = deploy.getResources().or(defaultResources).getCpus();
    double memoryMbPerInstance = deploy.getResources().or(defaultResources).getMemoryMb();

    checkBadRequest(cpusPerInstance > 0, "Request must have more than 0 cpus");
    checkBadRequest(memoryMbPerInstance > 0, "Request must have more than 0 memoryMb");

    checkBadRequest(cpusPerInstance <= maxCpusPerInstance, "Deploy %s uses too many cpus %s (maxCpusPerInstance %s in mesos configuration)", deploy.getId(), cpusPerInstance, maxCpusPerInstance);
    checkBadRequest(cpusPerInstance * instances <= maxCpusPerRequest,
        "Deploy %s uses too many cpus %s (%s*%s) (cpusPerRequest %s in mesos configuration)", deploy.getId(), cpusPerInstance * instances, cpusPerInstance, instances, maxCpusPerRequest);

    checkBadRequest(memoryMbPerInstance <= maxMemoryMbPerInstance,
        "Deploy %s uses too much memoryMb %s (maxMemoryMbPerInstance %s in mesos configuration)", deploy.getId(), memoryMbPerInstance, maxMemoryMbPerInstance);
    checkBadRequest(memoryMbPerInstance * instances <= maxMemoryMbPerRequest, "Deploy %s uses too much memoryMb %s (%s*%s) (maxMemoryMbPerRequest %s in mesos configuration)", deploy.getId(),
        memoryMbPerInstance * instances, memoryMbPerInstance, instances, maxMemoryMbPerRequest);
  }

  public SingularityRequest checkSingularityRequest(SingularityRequest request, Optional<SingularityRequest> existingRequest, Optional<SingularityDeploy> activeDeploy,
      Optional<SingularityDeploy> pendingDeploy) {

    checkBadRequest(request.getId() != null && !StringUtils.containsAny(request.getId(), JOINER.join(REQUEST_ID_ILLEGAL_CHARACTERS)), "Id can not be null or contain any of the following characters: %s", REQUEST_ID_ILLEGAL_CHARACTERS);
    checkBadRequest(request.getRequestType() != null, "RequestType cannot be null or missing");

    if (!allowRequestsWithoutOwners) {
      checkBadRequest(request.getOwners().isPresent() && !request.getOwners().get().isEmpty(), "Request must have owners defined (this can be turned off in Singularity configuration)");
    }

    checkBadRequest(request.getId().length() < maxRequestIdSize, "Request id must be less than %s characters, it is %s (%s)", maxRequestIdSize, request.getId().length(), request.getId());
    checkBadRequest(!request.getInstances().isPresent() || request.getInstances().get() > 0, "Instances must be greater than 0");

    checkBadRequest(request.getInstancesSafe() <= maxInstancesPerRequest,"Instances (%s) be greater than %s (maxInstancesPerRequest in mesos configuration)", request.getInstancesSafe(), maxInstancesPerRequest);

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

      final String originalSchedule = request.getQuartzScheduleSafe();

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

      checkBadRequest(isValidCronSchedule(quartzSchedule), "Schedule %s (from: %s) was not valid", quartzSchedule, originalSchedule);
    } else {
      checkBadRequest(!request.getQuartzSchedule().isPresent() && !request.getSchedule().isPresent(), "Non-scheduled requests can not specify a schedule");
      checkBadRequest(!request.getScheduleType().isPresent(), "ScheduleType can only be set for scheduled requests");
    }

    if (!request.isLongRunning()) {
      checkBadRequest(!request.isLoadBalanced(), "non-longRunning (scheduled/oneoff) requests can not be load balanced");
      checkBadRequest(!request.isRackSensitive(), "non-longRunning (scheduled/oneoff) requests can not be rack sensitive");
    } else {
      checkBadRequest(!request.getNumRetriesOnFailure().isPresent(), "NumRetriesOnFailure can only be set for non-long running requests");
      checkBadRequest(!request.getKillOldNonLongRunningTasksAfterMillis().isPresent(), "longRunning requests can not define a killOldNonLongRunningTasksAfterMillis value");
    }

    if (request.isScheduled()) {
      checkBadRequest(request.getInstances().or(1) == 1, "Scheduler requests can not be ran on more than one instance");
    } else if (request.isOneOff()) {
      checkBadRequest(!request.getInstances().isPresent(), "one-off requests can not define a # of instances");
    }

    return request.toBuilder().setQuartzSchedule(Optional.fromNullable(quartzSchedule)).build();
  }

  public SingularityWebhook checkSingularityWebhook(SingularityWebhook webhook) {
    checkNotNull(webhook, "Webhook is null");
    checkNotNull(webhook.getUri(), "URI is null");

    try {
      new URI(webhook.getUri());
    } catch (URISyntaxException e) {
      WebExceptions.badRequest("Invalid URI provided");
    }

    return webhook;
  }

  public SingularityDeploy checkDeploy(SingularityRequest request, SingularityDeploy deploy) {
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

    checkBadRequest(deployId != null && !StringUtils.containsAny(deployId, JOINER.join(DEPLOY_ID_ILLEGAL_CHARACTERS)), "Id must not be null and can not contain any of the following characters: %s", DEPLOY_ID_ILLEGAL_CHARACTERS);
    checkBadRequest(deployId.length() < maxDeployIdSize, "Deploy id must be less than %s characters, it is %s (%s)", maxDeployIdSize, deployId.length(), deployId);
    checkBadRequest(deploy.getRequestId() != null && deploy.getRequestId().equals(request.getId()), "Deploy id must match request id");

    if (request.isLoadBalanced()) {
      checkBadRequest(deploy.getServiceBasePath().isPresent(), "Deploy for loadBalanced request must include serviceBasePath");
      checkBadRequest(deploy.getLoadBalancerGroups().isPresent() && !deploy.getLoadBalancerGroups().get().isEmpty(), "Deploy for a loadBalanced request must include at least one load balacner group");
    }

    checkForIllegalResources(request, deploy);

    if (deploy.getResources().isPresent()) {
      if (deploy.getHealthcheckPortIndex().isPresent()) {
        checkBadRequest(deploy.getHealthcheckPortIndex().get() >= 0, "healthcheckPortIndex must be greater than 0");
        checkBadRequest(deploy.getResources().get().getNumPorts() > deploy.getHealthcheckPortIndex().get(), String
          .format("Must request %s ports for healthcheckPortIndex %s, only requested %s", deploy.getHealthcheckPortIndex().get() + 1, deploy.getHealthcheckPortIndex().get(),
            deploy.getResources().get().getNumPorts()));
      }
      if (deploy.getLoadBalancerPortIndex().isPresent()) {
        checkBadRequest(deploy.getLoadBalancerPortIndex().get() >= 0, "loadBalancerPortIndex must be greater than 0");
        checkBadRequest(deploy.getResources().get().getNumPorts() > deploy.getLoadBalancerPortIndex().get(), String
          .format("Must request %s ports for loadBalancerPortIndex %s, only requested %s", deploy.getLoadBalancerPortIndex().get() + 1, deploy.getLoadBalancerPortIndex().get(),
            deploy.getResources().get().getNumPorts()));
      }
    }

    checkBadRequest(deploy.getCommand().isPresent() && !deploy.getExecutorData().isPresent() ||
        deploy.getExecutorData().isPresent() && deploy.getCustomExecutorCmd().isPresent() && !deploy.getCommand().isPresent() ||
        deploy.getContainerInfo().isPresent(),
        "If not using custom executor, specify a command or containerInfo. If using custom executor, specify executorData and customExecutorCmd and no command.");

    checkBadRequest(!deploy.getContainerInfo().isPresent() || deploy.getContainerInfo().get().getType() != null, "Container type must not be null");

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

    return deploy;
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

      if (!dockerInfo.getPortMappings().isEmpty()) {
        checkBadRequest(dockerInfo.getNetwork().or(SingularityDockerNetworkType.HOST) == SingularityDockerNetworkType.BRIDGE,
            "Docker networking type must be BRIDGE if port mappings are set");
      }

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

  private final Pattern DAY_RANGE_REGEXP = Pattern.compile("[0-7]-[0-7]");
  private final Pattern COMMA_DAYS_REGEXP = Pattern.compile("([0-7],)+([0-7])?");

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

    checkBadRequest(split.length >= 4, "Schedule %s is invalid because it contained only %s splits (looking for at least 4)", schedule, split.length);

    List<String> newSchedule = Lists.newArrayListWithCapacity(6);

    boolean hasSeconds = split.length > 5;

    if (!hasSeconds) {
      newSchedule.add("0");
    } else {
      newSchedule.add(split[0]);
    }

    int indexMod = hasSeconds ? 1 : 0;

    newSchedule.add(split[indexMod + 0]);
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
        WebExceptions.badRequest("Schedule %s is invalid, day of week (%s) is not 0-7", schedule, dayOfWeekValue);
        break;
    }

    return newDayOfWeekValue;
  }

  private boolean isValidInteger(String strValue) {
    try {
      Integer.parseInt(strValue);
      return true;
    } catch (NumberFormatException nfe) {
      return false;
    }
  }

  public SingularityPriorityKillRequest checkSingularityPriorityKillRequest(SingularityPriorityKillRequest priorityKillRequest) {
    checkBadRequest(priorityKillRequest.getMinimumPriorityLevel() >= 0 && priorityKillRequest.getMinimumPriorityLevel() <= 1, "minimumPriorityLevel %s is invalid, must be between 0 and 1 (inclusive)", priorityKillRequest.getMinimumPriorityLevel());

    // auto-generate actionId if not set
    if (!priorityKillRequest.getActionId().isPresent()) {
      priorityKillRequest = new SingularityPriorityKillRequest(priorityKillRequest.getMinimumPriorityLevel(), priorityKillRequest.getMessage(), Optional.of(UUID.randomUUID().toString()));
    }

    return priorityKillRequest;
  }
}
