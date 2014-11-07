package com.hubspot.singularity.data;

import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

import org.apache.mesos.Protos;
import org.quartz.CronExpression;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityDockerInfo;
import com.hubspot.mesos.SingularityDockerPortMapping;
import com.hubspot.mesos.SingularityPortMappingType;
import com.hubspot.singularity.ScheduleType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.DeployHistoryHelper;

@Singleton
public class SingularityValidator {

  private static final Joiner JOINER = Joiner.on(" ");

  private final int maxDeployIdSize;
  private final int maxRequestIdSize;
  private final int maxCpusPerRequest;
  private final int maxCpusPerInstance;
  private final int maxInstancesPerRequest;
  private final int maxMemoryMbPerRequest;
  private final int defaultCpus;
  private final int defaultMemoryMb;
  private final int maxMemoryMbPerInstance;
  private final boolean allowRequestsWithoutOwners;
  private final DeployHistoryHelper deployHistoryHelper;
  private final Resources defaultResources;

  @Inject
  public SingularityValidator(SingularityConfiguration configuration, DeployHistoryHelper deployHistoryHelper) {
    this.maxDeployIdSize = configuration.getMaxDeployIdSize();
    this.maxRequestIdSize = configuration.getMaxRequestIdSize();
    this.allowRequestsWithoutOwners = configuration.isAllowRequestsWithoutOwners();
    this.deployHistoryHelper = deployHistoryHelper;

    this.defaultCpus = configuration.getMesosConfiguration().getDefaultCpus();
    this.defaultMemoryMb = configuration.getMesosConfiguration().getDefaultMemory();

    defaultResources = new Resources(defaultCpus, defaultMemoryMb, 0);

    this.maxCpusPerInstance = configuration.getMesosConfiguration().getMaxNumCpusPerInstance();
    this.maxCpusPerRequest = configuration.getMesosConfiguration().getMaxNumCpusPerRequest();
    this.maxMemoryMbPerInstance = configuration.getMesosConfiguration().getMaxMemoryMbPerInstance();
    this.maxMemoryMbPerRequest = configuration.getMesosConfiguration().getMaxMemoryMbPerRequest();
    this.maxInstancesPerRequest = configuration.getMesosConfiguration().getMaxNumInstancesPerRequest();
  }

  private void check(boolean expression, String message) {
    if (!expression) {
      throw WebExceptions.badRequest(message);
    }
  }

  private void checkForIllegalChanges(SingularityRequest request, SingularityRequest existingRequest) {
    check(request.isScheduled() == existingRequest.isScheduled(), "Request can not change whether it is a scheduled request");
    check(request.isDaemon() == existingRequest.isDaemon(), "Request can not change whether it is a daemon");
    check(request.isLoadBalanced() == existingRequest.isLoadBalanced(), "Request can not change whether it is load balanced");
  }

  private void checkForIllegalResources(SingularityRequest request, SingularityDeploy deploy) {
    int instances = request.getInstancesSafe();
    double cpusPerInstance = deploy.getResources().or(defaultResources).getCpus();
    double memoryMbPerInstance = deploy.getResources().or(defaultResources).getMemoryMb();

    check(cpusPerInstance > 0, "Request must have more than 0 cpus");
    check(memoryMbPerInstance > 0, "Request must have more than 0 memoryMb");

    check(cpusPerInstance <= maxCpusPerInstance, String.format("Deploy %s uses too many cpus %s (maxCpusPerInstance %s in mesos configuration)", deploy.getId(), cpusPerInstance, maxCpusPerInstance));
    check(cpusPerInstance * instances <= maxCpusPerRequest, String.format("Deploy %s uses too many cpus %s (%s*%s) (cpusPerRequest %s in mesos configuration)", deploy.getId(), cpusPerInstance * instances, cpusPerInstance, instances, maxCpusPerRequest));

    check(memoryMbPerInstance <= maxMemoryMbPerInstance, String.format("Deploy %s uses too much memoryMb %s (maxMemoryMbPerInstance %s in mesos configuration)", deploy.getId(), memoryMbPerInstance, maxMemoryMbPerInstance));
    check(memoryMbPerInstance * instances <= maxMemoryMbPerRequest, String.format("Deploy %s uses too much memoryMb %s (%s*%s) (maxMemoryMbPerRequest %s in mesos configuration)", deploy.getId(), memoryMbPerInstance * instances, memoryMbPerInstance, instances, maxMemoryMbPerRequest));
  }

  public SingularityRequest checkSingularityRequest(SingularityRequest request, Optional<SingularityRequest> existingRequest, Optional<SingularityDeploy> activeDeploy, Optional<SingularityDeploy> pendingDeploy) {
    check(request.getId() != null, "Id must not be null");

    if (!allowRequestsWithoutOwners) {
      check(request.getOwners().isPresent() && !request.getOwners().get().isEmpty(), "Request must have owners defined (this can be turned off in Singularity configuration)");
    }

    check(request.getId().length() < maxRequestIdSize, String.format("Request id must be less than %s characters, it is %s (%s)", maxRequestIdSize, request.getId().length(), request.getId()));
    check(!request.getInstances().isPresent() || request.getInstances().get() > 0, "Instances must be greater than 0");

    check(request.getInstancesSafe() <= maxInstancesPerRequest, String.format("Instances (%s) be greater than %s (maxInstancesPerRequest in mesos configuration)", request.getInstancesSafe(), maxInstancesPerRequest));

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
      final String originalSchedule = request.getQuartzScheduleSafe();

      check(request.getQuartzSchedule().isPresent() || request.getSchedule().isPresent(), "Specify at least one of schedule or quartzSchedule");

      check(!request.getDaemon().isPresent(), "Scheduled request must not set a daemon flag");
      check(request.getInstances().or(1) == 1, "Scheduled requests can not be ran on more than one instance");

      if (request.getQuartzSchedule().isPresent() && !request.getSchedule().isPresent()) {
        check(request.getScheduleType().or(ScheduleType.QUARTZ) == ScheduleType.QUARTZ, "If using quartzSchedule specify scheduleType QUARTZ or leave it blank");
      }

      if (request.getQuartzSchedule().isPresent() || (request.getScheduleType().isPresent() && request.getScheduleType().get() == ScheduleType.QUARTZ)) {
        quartzSchedule = originalSchedule;
      } else {
        check(request.getScheduleType().or(ScheduleType.CRON) == ScheduleType.CRON, "If not using quartzSchedule specify scheduleType CRON or leave it blank");
        check(!request.getQuartzSchedule().isPresent(), "If using schedule type CRON do not specify quartzSchedule");

        quartzSchedule = getQuartzScheduleFromCronSchedule(originalSchedule);
      }

      check(isValidCronSchedule(quartzSchedule), String.format("Schedule %s (from: %s) was not valid", quartzSchedule, originalSchedule));
    } else {
      check(!request.getScheduleType().isPresent(), "ScheduleType can only be set for scheduled requests");
      check(!request.getNumRetriesOnFailure().isPresent(), "NumRetriesOnFailure can only be set for scheduled requests");
    }

    if (!request.isLongRunning()) {
      check(!request.isLoadBalanced(), "non-longRunning (scheduled/oneoff) requests can not be load balanced");
      check(!request.isRackSensitive(), "non-longRunning (scheduled/oneoff) requests can not be rack sensitive");
    } else {
      check(!request.getKillOldNonLongRunningTasksAfterMillis().isPresent(), "longRunning requests can not define a killOldNonLongRunningTasksAfterMillis value");
    }

    if (request.isScheduled()) {
      check(request.getInstances().or(1) == 1, "Scheduler requests can not be ran on more than one instance");
    } else if (request.isOneOff()) {
      check(!request.getInstances().isPresent(), "one-off requests can not define a # of instances");
    }

    return request.toBuilder().setQuartzSchedule(Optional.fromNullable(quartzSchedule)).build();
  }

  public void checkDeploy(SingularityRequest request, SingularityDeploy deploy) {
    check(deploy.getId() != null && !deploy.getId().contains("-"), "Id must not be null and can not contain - characters");
    check(deploy.getId().length() < maxDeployIdSize, String.format("Deploy id must be less than %s characters, it is %s (%s)", maxDeployIdSize, deploy.getId().length(), deploy.getId()));
    check(deploy.getRequestId() != null && deploy.getRequestId().equals(request.getId()), "Deploy id must match request id");

    if (request.isLoadBalanced()) {
      check(deploy.getServiceBasePath().isPresent(), "Deploy for loadBalanced request must include serviceBasePath");
    }

    checkForIllegalResources(request, deploy);

    check((deploy.getCommand().isPresent() && !deploy.getExecutorData().isPresent()) ||
        (deploy.getExecutorData().isPresent() && deploy.getCustomExecutorCmd().isPresent() && !deploy.getCommand().isPresent() ||
            (deploy.getContainerInfo().isPresent())),
        "If not using custom executor, specify a command or containerInfo. If using custom executor, specify executorData and customExecutorCmd and no command.");

    check(!deploy.getContainerInfo().isPresent() || deploy.getContainerInfo().get().getType() != null, "Container type must not be null");

    if (deploy.getContainerInfo().isPresent() && deploy.getContainerInfo().get().getType() == Protos.ContainerInfo.Type.DOCKER) {
      checkDocker(deploy);
    }

    check(deployHistoryHelper.isDeployIdAvailable(request.getId(), deploy.getId()), "Can not deploy a deploy that has already been deployed");
  }

  private void checkDocker(SingularityDeploy deploy) {
    if (deploy.getResources().isPresent() && deploy.getContainerInfo().get().getDocker().isPresent()) {
      final SingularityDockerInfo dockerInfo = deploy.getContainerInfo().get().getDocker().get();
      final int numPorts = deploy.getResources().get().getNumPorts();

      if (!dockerInfo.getPortMappings().isEmpty()) {
        check(dockerInfo.getNetwork().or(Protos.ContainerInfo.DockerInfo.Network.HOST) == Protos.ContainerInfo.DockerInfo.Network.BRIDGE, "Docker networking type must be BRIDGE if port mappings are set");
      }

      for (SingularityDockerPortMapping portMapping : dockerInfo.getPortMappings()) {
        if (portMapping.getContainerPortType() == SingularityPortMappingType.FROM_OFFER) {
          check(portMapping.getContainerPort() >= 0 && portMapping.getContainerPort() < numPorts, String.format("Index of port resource for containerPort must be between 0 and %d (inclusive)", numPorts - 1));
        }

        if (portMapping.getHostPortType() == SingularityPortMappingType.FROM_OFFER) {
          check(portMapping.getHostPort() >= 0 && portMapping.getHostPort() < numPorts, String.format("Index of port resource for hostPort must be between 0 and %d (inclusive)", numPorts - 1));
        }
      }
    }
  }

  private boolean isValidCronSchedule(String schedule) {
    return CronExpression.isValidExpression(schedule);
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
  private String getQuartzScheduleFromCronSchedule(String schedule) {
    if (schedule == null) {
      return null;
    }

    String[] split = schedule.split(" ");

    if (split.length < 4) {
      throw WebExceptions.badRequest("Schedule %s is invalid because it contained only %s splits (looking for at least 4)", schedule, split.length);
    }

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

    // standard cron is 0-6, quartz is 1-7
    // therefore, we should add 1 to any values between 0-6. 7 in a standard cron is sunday,
    // which is sat in quartz. so if we get a value of 7, we should change it to 1.
    if (isValidInteger(dayOfWeek)) {
      int dayOfWeekValue = Integer.parseInt(dayOfWeek);

      if (dayOfWeekValue < 0 || dayOfWeekValue > 7) {
        throw WebExceptions.badRequest("Schedule %s is invalid, day of week (%s) is not 0-7", schedule, dayOfWeekValue);
      }

      if (dayOfWeekValue == 7) {
        dayOfWeekValue = 1;
      } else {
        dayOfWeekValue++;
      }

      dayOfWeek = Integer.toString(dayOfWeekValue);
    }

    newSchedule.add(dayOfMonth);
    newSchedule.add(split[indexMod + 3]);
    newSchedule.add(dayOfWeek);

    return JOINER.join(newSchedule);
  }

  private boolean isValidInteger(String strValue) {
    try {
      Integer.parseInt(strValue);
      return true;
    } catch (NumberFormatException nfe) {
      return false;
    }
  }

}
