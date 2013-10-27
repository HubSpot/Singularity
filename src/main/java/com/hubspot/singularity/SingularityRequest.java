package com.hubspot.singularity;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.quartz.CronExpression;

import com.codahale.dropwizard.validation.ValidationMethod;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.hubspot.mesos.Resources;

public class SingularityRequest {
  private static final Joiner JOINER = Joiner.on(" ");

  @NotNull
  private final String name;

  private final String executor;
  private final Resources resources;

  private final String schedule;

  @Min(0)
  private final Integer instances;
  private final Boolean rackSensitive;
  private final Boolean daemon;

  private final String command;
  private final Map<String, String> args;
  private final List<String> uris;
  private final Object executorData;

  @JsonIgnore
  @ValidationMethod(message="Scheduled requests can not be ran on more than one instance, and must not be daemons")
  public boolean isScheduleInstancesValid() {
    return schedule == null || ((instances == null || instances == 0) && (daemon == null || !daemon));
  }

  @JsonIgnore
  @ValidationMethod(message="Non-daemons can not be ran on more than one instance")
  public boolean isNonDaemonInstancesValid() {
    return (daemon == null || daemon) || (instances == null || instances == 0);
  }

  @JsonIgnore
  @ValidationMethod(message="Cron schedule was not parseable")
  public boolean isCronScheduleValid() {
    return schedule == null || CronExpression.isValidExpression(schedule);
  }

  @JsonIgnore
  @ValidationMethod(message="If not using custom executor, specify a command. If using custom executor, specify executorData OR command.")
  public boolean isExecutorValid() {
    return (command != null && executorData == null) || (executorData != null && executor != null && command == null);
  }

  @JsonIgnore
  @ValidationMethod(message="Requiring ports requires a custom executor with a json executor data payload OR not using a custom executor")
  public boolean isPortsValid() {
    return resources == null || resources.getNumPorts() == 0 || (executor == null || (executorData != null && executorData instanceof Map));
  }

  @JsonCreator
  public SingularityRequest(@JsonProperty("command") String command, @JsonProperty("name") String name, @JsonProperty("executor") String executor, @JsonProperty("resources") Resources resources, @JsonProperty("schedule") String schedule,
      @JsonProperty("instances") Integer instances, @JsonProperty("daemon") Boolean daemon, @JsonProperty("args") Map<String, String> args, @JsonProperty("uris") List<String> uris,
      @JsonProperty("executorData") Object executorData, @JsonProperty("rackSensitive") Boolean rackSensitive) {
    schedule = adjustSchedule(schedule);

    this.command = command;
    this.name = name;
    this.resources = resources;
    this.executor = executor;
    this.schedule = schedule;
    this.daemon = daemon;
    this.instances = instances;
    this.rackSensitive = rackSensitive;
    
    this.args = args;
    this.uris = uris;
    this.executorData = executorData;
  }

  public Map<String, String> getArgs() {
    return args;
  }

  public List<String> getUris() {
    return uris;
  }

  public Object getExecutorData() {
    return executorData;
  }
  
  public Boolean getRackSensitive() {
    return rackSensitive;
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
    } else if (!dayOfWeek.equals("?")) {
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

  @Override
  public String toString() {
    return "SingularityRequest [name=" + name + ", executor=" + executor + ", resources=" + resources + ", schedule=" + schedule + ", instances=" + instances + ", rackSensitive=" + rackSensitive + ", daemon=" + daemon + ", command="
        + command + ", args=" + args + ", uris=" + uris + ", executorData=" + executorData + "]";
  }
  
}
