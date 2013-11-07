package com.hubspot.singularity;

import java.util.List;
import java.util.Map;

import com.hubspot.mesos.Resources;

public class SingularityRequestBuilder {

  private String name;

  private String executor;
  private Resources resources;

  private String schedule;

  private Integer instances;
  private Boolean rackSensitive;
  private Boolean daemon;

  private String command;
  private Map<String, String> env;
  private List<String> uris;
  private Object executorData;

  public SingularityRequest build() {
    return new SingularityRequest(command, name, executor, resources, schedule, instances, daemon, env, uris, executorData, rackSensitive);
  }
  
  public String getName() {
    return name;
  }

  public SingularityRequestBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public String getExecutor() {
    return executor;
  }

  public SingularityRequestBuilder setExecutor(String executor) {
    this.executor = executor;
    return this;
  }

  public Resources getResources() {
    return resources;
  }

  public SingularityRequestBuilder setResources(Resources resources) {
    this.resources = resources;
    return this;
  }

  public String getSchedule() {
    return schedule;
  }

  public SingularityRequestBuilder setSchedule(String schedule) {
    this.schedule = schedule;
    return this;
  }

  public Integer getInstances() {
    return instances;
  }

  public SingularityRequestBuilder setInstances(Integer instances) {
    this.instances = instances;
    return this;
  }

  public Boolean getRackSensitive() {
    return rackSensitive;
  }

  public SingularityRequestBuilder setRackSensitive(Boolean rackSensitive) {
    this.rackSensitive = rackSensitive;
    return this;
  }

  public Boolean getDaemon() {
    return daemon;
  }

  public SingularityRequestBuilder setDaemon(Boolean daemon) {
    this.daemon = daemon;
    return this;
  }

  public String getCommand() {
    return command;
  }

  public SingularityRequestBuilder setCommand(String command) {
    this.command = command;
    return this;
  }

  public Map<String, String> getEnv() {
    return env;
  }

  public SingularityRequestBuilder setEnv(Map<String, String> env) {
    this.env = env;
    return this;
  }

  public List<String> getUris() {
    return uris;
  }

  public SingularityRequestBuilder setUris(List<String> uris) {
    this.uris = uris;
    return this;
  }
  
  public Object getExecutorData() {
    return executorData;
  }

  public SingularityRequestBuilder setExecutorData(Object executorData) {
    this.executorData = executorData;
    return this;
  }

}
