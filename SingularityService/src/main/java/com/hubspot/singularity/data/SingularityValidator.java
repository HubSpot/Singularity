package com.hubspot.singularity.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.WebExceptions.checkForbidden;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Singleton;

import org.apache.mesos.Protos;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityDockerInfo;
import com.hubspot.mesos.SingularityDockerPortMapping;
import com.hubspot.mesos.SingularityPortMappingType;
import com.hubspot.singularity.ScheduleType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.DeployHistoryHelper;
import com.hubspot.singularity.ldap.SingularityLDAPManager;

@Singleton
public class SingularityValidator {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityValidator.class);

  private static final Joiner JOINER = Joiner.on(" ");
  private static final Joiner COMMA_JOINER = Joiner.on(", ");

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
  private final boolean createDeployIds;
  private final int deployIdLength;
  private final DeployHistoryHelper deployHistoryHelper;
  private final Resources defaultResources;
  private final boolean ldapEnabled;
  private final ImmutableSet<String> ldapRequiredGroups;
  private final ImmutableSet<String> ldapAdminGroups;
  private final SingularityLDAPManager ldapManager;

  @Inject
  public SingularityValidator(SingularityConfiguration configuration, DeployHistoryHelper deployHistoryHelper, SingularityLDAPManager ldapManager) {
    this.maxDeployIdSize = configuration.getMaxDeployIdSize();
    this.maxRequestIdSize = configuration.getMaxRequestIdSize();
    this.allowRequestsWithoutOwners = configuration.isAllowRequestsWithoutOwners();
    this.createDeployIds = configuration.isCreateDeployIds();
    this.deployIdLength = configuration.getDeployIdLength();
    this.deployHistoryHelper = deployHistoryHelper;

    this.defaultCpus = configuration.getMesosConfiguration().getDefaultCpus();
    this.defaultMemoryMb = configuration.getMesosConfiguration().getDefaultMemory();

    defaultResources = new Resources(defaultCpus, defaultMemoryMb, 0);

    this.maxCpusPerInstance = configuration.getMesosConfiguration().getMaxNumCpusPerInstance();
    this.maxCpusPerRequest = configuration.getMesosConfiguration().getMaxNumCpusPerRequest();
    this.maxMemoryMbPerInstance = configuration.getMesosConfiguration().getMaxMemoryMbPerInstance();
    this.maxMemoryMbPerRequest = configuration.getMesosConfiguration().getMaxMemoryMbPerRequest();
    this.maxInstancesPerRequest = configuration.getMesosConfiguration().getMaxNumInstancesPerRequest();

    this.ldapEnabled = configuration.getLdapConfiguration().isEnabled();
    this.ldapManager = ldapManager;
    this.ldapRequiredGroups = ImmutableSet.copyOf(configuration.getLdapConfiguration().getRequiredGroups());
    this.ldapAdminGroups = ImmutableSet.copyOf(configuration.getLdapConfiguration().getAdminGroups());
  }

  public void checkForAuthorization(SingularityRequest request, Optional<SingularityRequest> existingRequest, Optional<String> user) {
    if (ldapEnabled) {
      checkBadRequest(user.isPresent(), "user must be present");

      final Set<String> groups = ldapManager.getGroupsForUser(user.get());

      // check for required group membership...
      if (!ldapRequiredGroups.isEmpty()) {
        checkForbidden(!Sets.intersection(groups, ldapRequiredGroups).isEmpty(), "User %s must be part of one or more required groups: %s", user.get(), COMMA_JOINER.join(ldapRequiredGroups));
      }

      // if user isn't part of an admin group...
      if (Sets.intersection(groups, ldapAdminGroups).isEmpty()) {
        // if changing groups, check for group membership of old group
        if (existingRequest.isPresent() && existingRequest.get().getGroup().isPresent() && !request.getGroup().equals(existingRequest.get().getGroup())) {
          checkForbidden(groups.contains(existingRequest.get().getGroup().get()), "User %s must be part of old group %s", user.get(), existingRequest.get().getGroup().get());
        }

        // check for group membership of current / new group
        if (request.getGroup().isPresent()) {
          checkForbidden(groups.contains(request.getGroup().get()), "User %s must be part of group %s", user.get(), request.getGroup().get());
        }
      }
    }
  }

  public void checkForAdminAuthorization(Optional<String> user) {
    if (ldapEnabled) {
      if (!ldapAdminGroups.isEmpty()) {
        checkBadRequest(user.isPresent(), "user must be present");

        final Set<String> groups = ldapManager.getGroupsForUser(user.get());

        checkForbidden(!Sets.intersection(groups, ldapAdminGroups).isEmpty(), "User %s must be part of an admin group", user);
      }
    }
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
      Optional<SingularityDeploy> pendingDeploy, Optional<String> user) {
    checkForAuthorization(request, existingRequest, user);

    checkBadRequest(request.getId() != null && !request.getId().contains("/"), "Id can not be null or contain / characters");

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
      final String originalSchedule = request.getQuartzScheduleSafe();

      checkBadRequest(request.getQuartzSchedule().isPresent() || request.getSchedule().isPresent(), "Specify at least one of schedule or quartzSchedule");


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

  public SingularityDeploy checkDeploy(SingularityRequest request, SingularityDeploy deploy, Optional<String> user) {
    checkNotNull(request, "request is null");
    checkNotNull(deploy, "deploy is null");

    checkForAuthorization(request, Optional.<SingularityRequest>absent(), user);

    String deployId = deploy.getId();

    if (deployId == null) {
      checkBadRequest(createDeployIds, "Id must not be null");
      SingularityDeployBuilder builder = deploy.toBuilder();
      builder.setId(createUniqueDeployId());
      deploy = builder.build();
      deployId = deploy.getId();
    }

    checkBadRequest(!deployId.contains("/") && !deployId.contains("-"), "Id must not be null and can not contain / or - characters");
    checkBadRequest(deployId.length() < maxDeployIdSize, "Deploy id must be less than %s characters, it is %s (%s)", maxDeployIdSize, deployId.length(), deployId);
    checkBadRequest(deploy.getRequestId() != null && deploy.getRequestId().equals(request.getId()), "Deploy id must match request id");

    if (request.isLoadBalanced()) {
      checkBadRequest(deploy.getServiceBasePath().isPresent(), "Deploy for loadBalanced request must include serviceBasePath");
    }

    checkForIllegalResources(request, deploy);

    checkBadRequest(deploy.getCommand().isPresent() && !deploy.getExecutorData().isPresent() ||
        deploy.getExecutorData().isPresent() && deploy.getCustomExecutorCmd().isPresent() && !deploy.getCommand().isPresent() ||
        deploy.getContainerInfo().isPresent(),
        "If not using custom executor, specify a command or containerInfo. If using custom executor, specify executorData and customExecutorCmd and no command.");

    checkBadRequest(!deploy.getContainerInfo().isPresent() || deploy.getContainerInfo().get().getType() != null, "Container type must not be null");

    if (deploy.getContainerInfo().isPresent() && deploy.getContainerInfo().get().getType() == Protos.ContainerInfo.Type.DOCKER) {
      checkDocker(deploy);
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

      if (!dockerInfo.getPortMappings().isEmpty()) {
        checkBadRequest(dockerInfo.getNetwork().or(Protos.ContainerInfo.DockerInfo.Network.HOST) == Protos.ContainerInfo.DockerInfo.Network.BRIDGE,
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

    // standard cron is 0-6, quartz is 1-7
    // therefore, we should add 1 to any values between 0-6. 7 in a standard cron is sunday,
    // which is sat in quartz. so if we get a value of 7, we should change it to 1.
    if (isValidInteger(dayOfWeek)) {
      int dayOfWeekValue = Integer.parseInt(dayOfWeek);

      checkBadRequest(dayOfWeekValue >= 0 && dayOfWeekValue <= 7, "Schedule %s is invalid, day of week (%s) is not 0-7", schedule, dayOfWeekValue);

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
