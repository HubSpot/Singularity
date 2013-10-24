package com.hubspot.singularity;

import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.quartz.CronExpression;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hubspot.mesos.Resources;

public class SingularityRequest {
  
  @NotNull
  private final String command;
  
  @NotNull
  private final String name;
  
  private final String executor;
  private final Resources resources;
  
  private final String schedule;
  
  @Min(0)
  private final Integer instances;
  private final Boolean daemon;
  
  private static final Joiner JOINER = Joiner.on(" ");
  
  @JsonCreator
  public SingularityRequest(@JsonProperty("command") String command, @JsonProperty("name") String name, @JsonProperty("executor") String executor, @JsonProperty("resources") Resources resources, @JsonProperty("schedule") String schedule, @JsonProperty("instances") Integer instances, @JsonProperty("daemon") Boolean daemon) {
    schedule = adjustSchedule(schedule);
    
    Preconditions.checkState(schedule == null || ((instances == null || instances == 0) && (daemon == null || !daemon)), "Scheduled requests can not be ran on more than one instance, and must not be daemons");
    Preconditions.checkState((daemon == null || daemon) || (instances == null || instances == 0), "Non-daemons can not be ran on more than one instance");
    Preconditions.checkState(schedule == null || CronExpression.isValidExpression(schedule), "Cron Schedule %s was not parseable", schedule);
    
    this.command = command;
    this.name = name;
    this.resources = resources;
    this.executor = executor;
    this.schedule = schedule;
    this.daemon = daemon;
    this.instances = instances;
  }
  
 /**
  * 
  *  Transforms unix cron into fucking quartz cron; adding seconds if not passed in and switching either day of month or day of week to ? 
  *  
  *   Field Name              Allowed Values          Allowed Special Characters
  *   Seconds         0-59            , - * /
  *   Minutes         0-59            , - * /
  *   Hours           0-23            , - * /
  *   Day-of-month            1-31            , - * ? / L W
  *   Month           1-12 or JAN-DEC         , - * /
  *   Day-of-Week             1-7 or SUN-SAT          , - * ? / L #
  *   Year (Optional)         empty, 1970-2199                , - * /
  */
  private String adjustSchedule(String schedule) {
    if (schedule == null) {
      return null;
    }
    
    String[] split = schedule.split(" ");
    
    if (split.length < 4) { 
      throw new IllegalStateException(String.format("Schedule %s is invalid", schedule));
    }
    
    List<String> newSchedule = Lists.newArrayListWithCapacity(6);
    
    boolean hasSeconds = split.length > 5;
    
    if (!hasSeconds) {
      newSchedule.add("*");
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
    } else {
      dayOfMonth = "?";
    }

    newSchedule.add(dayOfMonth);
    newSchedule.add(split[indexMod + 3]);
    newSchedule.add(dayOfWeek);
    
    return JOINER.join(newSchedule);
  }
  
  
  public byte[] getRequestData(ObjectMapper objectMapper) throws Exception {
    return objectMapper.writeValueAsBytes(this);
  }

  public static SingularityRequest getRequestFromData(byte[] request, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(request, SingularityRequest.class);
  }
  
  public Integer getInstances() {
    return instances;
  }

  public Boolean getDaemon() {
    return daemon;
  }
  
  @JsonIgnore
  public boolean alwaysRunning() {
    return (daemon == null || daemon.booleanValue()) && !isScheduled();
  }
  
  @JsonIgnore
  public boolean isScheduled() {
    return schedule != null;
  }

  public String getSchedule() {
    return schedule;
  }

  public String getName() {
    return name;
  }

  public String getExecutor() {
    return executor;
  }

  public Resources getResources() {
    return resources;
  }

  public String getCommand() {
    return command;
  }

}
