package com.hubspot.singularity.data;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.quartz.CronExpression;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.HistoryManager;

public class SingularityValidator {

  private static final Joiner JOINER = Joiner.on(" ");
  
  private final int maxDeployIdSize;
  private final int maxRequestIdSize;
  private final boolean allowRequestsWithoutOwners;
  private final HistoryManager historyManager;
  private final DeployManager deployManager;
  
  @Inject
  public SingularityValidator(SingularityConfiguration configuration, DeployManager deployManager, HistoryManager historyManager) {
    this.maxDeployIdSize = configuration.getMaxDeployIdSize();
    this.maxRequestIdSize = configuration.getMaxRequestIdSize();
    this.allowRequestsWithoutOwners = configuration.isAllowRequestsWithoutOwners();
    this.deployManager = deployManager;
    this.historyManager = historyManager;
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
  
  public SingularityRequest checkSingularityRequest(SingularityRequest request, Optional<SingularityRequest> existingRequest) {
    check(request.getId() != null, "Id must not be null");
    
    if (!allowRequestsWithoutOwners) {
      check(request.getOwners().isPresent() && !request.getOwners().get().isEmpty(), "Request must have owners defined (this can be turned off in Singularity configuration)");
    }
    
    check(request.getId().length() < maxRequestIdSize, String.format("Request id must be less than %s characters, it is %s (%s)", maxRequestIdSize, request.getId().length(), request.getId()));
    check(!request.getInstances().isPresent() || request.getInstances().get() > 0, "Instances must be greater than 0");
    
    if (existingRequest.isPresent()) {
      checkForIllegalChanges(request, existingRequest.get());
    }
    
    String newSchedule = null;
    
    if (request.isScheduled()) {
      check(!request.getDaemon().isPresent(), "Scheduled request must not set a daemon flag");
      check(request.getInstances().or(1) == 1, "Scheduled requests can not be ran on more than one instance");

      newSchedule = adjustSchedule(request.getSchedule().get());

      check(isValidCronSchedule(newSchedule), String.format("Cron schedule %s (adjusted: %s) was not parseable", request.getSchedule(), newSchedule));
    } else {
      check(!request.getNumRetriesOnFailure().isPresent(), "NumRetriesOnFailure can only be set for scheduled requests");
    }
    
    if (request.isLoadBalanced()) {
      check(!request.isOneOff() && !request.isScheduled(), "Scheduled or one-off requests can not be load balanced");
    }
    
    if (!request.isLongRunning()) {
      check(request.getInstances().or(1) == 1, "Non-daemons can not be ran on more than one instance");
    }
    
    return request.toBuilder().setSchedule(Optional.fromNullable(newSchedule)).build();
  }
  
  public void checkDeploy(SingularityRequest request, SingularityDeploy deploy) {
    check(deploy.getId() != null && !deploy.getId().contains("-"), "Id must not be null and can not contain - characters");
    check(deploy.getId().length() < maxDeployIdSize, String.format("Deploy id must be less than %s characters, it is %s (%s)", maxDeployIdSize, deploy.getId().length(), deploy.getId()));
    check(deploy.getRequestId() != null && deploy.getRequestId().equals(request.getId()), "Deploy id must match request id");
    
    if (request.isLoadBalanced()) {
      check(deploy.getServiceBasePath().isPresent(), "Deploy for loadBalanced request must include serviceBasePath");
    }
    
    check((deploy.getCommand().isPresent() && !deploy.getExecutorData().isPresent()) || (deploy.getExecutorData().isPresent() && deploy.getCustomExecutorCmd().isPresent() && !deploy.getCommand().isPresent()), 
        "If not using custom executor, specify a command. If using custom executor, specify executorData and customExecutorCmd and no command.");

    check(!deployManager.getDeploy(request.getId(), deploy.getId()).isPresent() && !historyManager.getDeployHistory(request.getId(), deploy.getId()).isPresent(), "Can not deploy a deploy that has already been deployed");
  }
  
  private boolean isValidCronSchedule(String schedule) {
    if (!CronExpression.isValidExpression(schedule)) {
      return false;
    }
    
    try {
      CronExpression ce = new CronExpression(schedule);
      
      if (ce.getNextValidTimeAfter(new Date()) == null) {
        return false;
      }
      
    } catch (ParseException pe) {
      return false;
    }
     
    return true;
  }
  
  /**
   * 
   * Transforms unix cron into fucking quartz cron; adding seconds if not passed
   * in and switching either day of month or day of week to ?
   * 
   * Field Name Allowed Values Allowed Special Characters Seconds 0-59 , - * /
   * Minutes 0-59 , - * / Hours 0-23 , - * / Day-of-month 1-31 , - * ? / L W
   * Month 1-12 or JAN-DEC , - * / Day-of-Week 1-7 or SUN-SAT , - * ? / L # Year
   * (Optional) empty, 1970-2199 , - * /
   */
  private String adjustSchedule(String schedule) {
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
