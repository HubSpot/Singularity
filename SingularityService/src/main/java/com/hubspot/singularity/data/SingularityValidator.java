package com.hubspot.singularity.data;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.quartz.CronExpression;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.WebExceptions;

public class SingularityValidator {

  private static final Joiner JOINER = Joiner.on(" ");
  
  // TODO max ID sizes?
//  private final int maxDeployIdSize;
//  
//  public SingularityDeployValidator(SingularityConfiguration configuration) {
//    this.request = request;
//  }
  
  private void check(boolean expression, String message) {
    if (!expression) {
      throw WebExceptions.badRequest(message);
    }
  }
  
  public SingularityRequest checkSingularityRequest(SingularityRequest request) {
    // TODO change checks. ??
    
    check(request.getId() != null, "Id must not be null");
    check(!request.getInstances().isPresent() || request.getInstances().get() > 0, "Instances must be greater than 0");
    
    String newSchedule = null;
    
    if (request.isScheduled()) {
      check(!request.getDaemon().isPresent(), "Scheduled request must not set a daemon flag");
      check(request.getInstances().or(1) == 1, "Scheduled requests can not be ran on more than one instance");

      newSchedule = adjustSchedule(request.getSchedule().get());

      check(isValidCronSchedule(newSchedule), String.format("Cron schedule %s (adjusted: %s) was not parseable", request.getSchedule(), newSchedule));
    } else {
      check(!request.getNumRetriesOnFailure().isPresent(), "NumRetriesOnFailure can only be set for scheduled requests");
    }
    
    if (!request.isLongRunning()) {
      check(request.getInstances().or(1) == 1, "Non-daemons can not be ran on more than one instance");
    }
    
//  checkRequestState(request.getId().length() < MAX_REQUEST_ID_SIZE, String.format("Request id must be less than %s characters, it is %s (%s)", MAX_REQUEST_ID_SIZE, request.getId().length(), request.getId()));

    return request.toBuilder().setSchedule(Optional.fromNullable(newSchedule)).build();
  }
  
  public void checkDeploy(SingularityRequest request, SingularityDeploy deploy) {
    check(deploy.getId() != null && !deploy.getId().contains("-"), "Id must not be null and can not contain - characters");
    check(deploy.getRequestId() != null && deploy.getRequestId().equals(request.getId()), "Deploy id must match request id");
    
    check((deploy.getCommand().isPresent() && !deploy.getExecutorData().isPresent()) || (deploy.getExecutorData().isPresent() && deploy.getExecutor().isPresent() && !deploy.getCommand().isPresent()), 
        "If not using custom executor, specify a command. If using custom executor, specify executorData OR command.");
    check(!deploy.getResources().isPresent() || deploy.getResources().get().getNumPorts() == 0 || (!deploy.getExecutor().isPresent() || (deploy.getExecutorData().isPresent() && deploy.getExecutorData().get() instanceof Map)), 
        "Requiring ports requires a custom executor with a json executor data payload OR not using a custom executor");
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
