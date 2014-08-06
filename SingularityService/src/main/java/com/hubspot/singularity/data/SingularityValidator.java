package com.hubspot.singularity.data;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.quartz.CronExpression;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.HistoryManager;

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
  private final HistoryManager historyManager;
  private final DeployManager deployManager;
  private final Resources DEFAULT_RESOURCES;
  
  @Inject
  public SingularityValidator(final SingularityConfiguration configuration, final DeployManager deployManager, final HistoryManager historyManager) {
    this.maxDeployIdSize = configuration.getMaxDeployIdSize();
    this.maxRequestIdSize = configuration.getMaxRequestIdSize();
    this.allowRequestsWithoutOwners = configuration.isAllowRequestsWithoutOwners();
    this.deployManager = deployManager;
    this.historyManager = historyManager;

    this.defaultCpus = configuration.getMesosConfiguration().getDefaultCpus();
    this.defaultMemoryMb = configuration.getMesosConfiguration().getDefaultMemory();
    
    DEFAULT_RESOURCES = new Resources(defaultCpus, defaultMemoryMb, 0);
    
    this.maxCpusPerInstance = configuration.getMesosConfiguration().getMaxNumCpusPerInstance();
    this.maxCpusPerRequest = configuration.getMesosConfiguration().getMaxNumCpusPerRequest();
    this.maxMemoryMbPerInstance = configuration.getMesosConfiguration().getMaxMemoryMbPerInstance();
    this.maxMemoryMbPerRequest = configuration.getMesosConfiguration().getMaxMemoryMbPerRequest();
    this.maxInstancesPerRequest = configuration.getMesosConfiguration().getMaxNumInstancesPerRequest();
  }
  
  private void check(final boolean expression, final String message) {
    if (!expression) {
      throw WebExceptions.badRequest(message);
    }
  }
  
  private void checkForIllegalChanges(final SingularityRequest request, final SingularityRequest existingRequest) {
    check(request.isScheduled() == existingRequest.isScheduled(), "Request can not change whether it is a scheduled request");
    check(request.isDaemon() == existingRequest.isDaemon(), "Request can not change whether it is a daemon");
    check(request.isLoadBalanced() == existingRequest.isLoadBalanced(), "Request can not change whether it is load balanced");
  }

  private void checkForIllegalResources(final SingularityRequest request, final SingularityDeploy deploy) {
    final int instances = request.getInstancesSafe();
    final double cpusPerInstance = deploy.getResources().or(DEFAULT_RESOURCES).getCpus();
    final double memoryMbPerInstance = deploy.getResources().or(DEFAULT_RESOURCES).getMemoryMb();
    
    check(cpusPerInstance > 0, "Request must have more than 0 cpus");
    check(memoryMbPerInstance > 0, "Request must have more than 0 memoryMb");
    
    check(cpusPerInstance <= maxCpusPerInstance, String.format("Deploy %s uses too many cpus %s (maxCpusPerInstance %s in mesos configuration)", deploy.getId(), cpusPerInstance, maxCpusPerInstance));
    check(cpusPerInstance * instances <= maxCpusPerRequest, String.format("Deploy %s uses too many cpus %s (%s*%s) (cpusPerRequest %s in mesos configuration)", deploy.getId(), cpusPerInstance * instances, cpusPerInstance, instances, maxCpusPerRequest));
    
    check(memoryMbPerInstance <= maxMemoryMbPerInstance, String.format("Deploy %s uses too much memoryMb %s (maxMemoryMbPerInstance %s in mesos configuration)", deploy.getId(), memoryMbPerInstance, maxMemoryMbPerInstance));
    check(memoryMbPerInstance * instances <= maxMemoryMbPerRequest, String.format("Deploy %s uses too much memoryMb %s (%s*%s) (maxMemoryMbPerRequest %s in mesos configuration)", deploy.getId(), memoryMbPerInstance * instances, memoryMbPerInstance, instances, maxMemoryMbPerRequest));
  }
  
  public SingularityRequest checkSingularityRequest(final SingularityRequest request, final Optional<SingularityRequest> existingRequest, final Optional<SingularityDeploy> activeDeploy, final Optional<SingularityDeploy> pendingDeploy) {
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
    
    String newSchedule = null;
    
    if (request.isScheduled()) {
      check(!request.getDaemon().isPresent(), "Scheduled request must not set a daemon flag");
      check(request.getInstances().or(1) == 1, "Scheduled requests can not be ran on more than one instance");

      newSchedule = request.getSchedule().get();

      check(isValidCronSchedule(newSchedule), String.format("Cron schedule %s (adjusted: %s) was not parseable", request.getSchedule(), newSchedule));
    } else {
      check(!request.getNumRetriesOnFailure().isPresent(), "NumRetriesOnFailure can only be set for scheduled requests");
    }
    
    if (!request.isLongRunning()) {
      check(!request.isLoadBalanced(), "non-long-running (scheduled/oneoff) requests can not be load balanced");
      check(!request.isRackSensitive(), "non-long-running (scheduled/oneoff) requests can not be rack sensitive");
    }
    
    if (request.isScheduled()) {
      check(request.getInstances().or(1) == 1, "Scheduler requests can not be ran on more than one instance");
    } else if (request.isOneOff()) {
      check(!request.getInstances().isPresent(), "one-off requests can not define a # of instances");
    }
    
    return request.toBuilder().setSchedule(Optional.fromNullable(newSchedule)).build();
  }
  
  public void checkDeploy(final SingularityRequest request, final SingularityDeploy deploy) {
    check(deploy.getId() != null && !deploy.getId().contains("-"), "Id must not be null and can not contain - characters");
    check(deploy.getId().length() < maxDeployIdSize, String.format("Deploy id must be less than %s characters, it is %s (%s)", maxDeployIdSize, deploy.getId().length(), deploy.getId()));
    check(deploy.getRequestId() != null && deploy.getRequestId().equals(request.getId()), "Deploy id must match request id");
    
    if (request.isLoadBalanced()) {
      check(deploy.getServiceBasePath().isPresent(), "Deploy for loadBalanced request must include serviceBasePath");
    }
    
    checkForIllegalResources(request, deploy);
    
    check((deploy.getCommand().isPresent() && !deploy.getExecutorData().isPresent()) || (deploy.getExecutorData().isPresent() && deploy.getCustomExecutorCmd().isPresent() && !deploy.getCommand().isPresent()), 
        "If not using custom executor, specify a command. If using custom executor, specify executorData and customExecutorCmd and no command.");

    check(!deployManager.getDeploy(request.getId(), deploy.getId()).isPresent() && !historyManager.getDeployHistory(request.getId(), deploy.getId()).isPresent(), "Can not deploy a deploy that has already been deployed");
  }
  
  private boolean isValidCronSchedule(final String schedule) {
    if (!CronExpression.isValidExpression(schedule)) {
      return false;
    }
    
    try {
      final CronExpression ce = new CronExpression(schedule);
      
      if (ce.getNextValidTimeAfter(new Date()) == null) {
        return false;
      }
      
    } catch (final ParseException pe) {
      return false;
    }
     
    return true;
  }
  
}
