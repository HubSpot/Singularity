package com.hubspot.singularity;

import java.util.List;
import java.util.Map;

import com.hubspot.mesos.Resources;

public class SingularityRequestBuilder {

  private String id;

  private String name;
  private String version;
  private Long timestamp;
  private Map<String, String> metadata;
  
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
    return new SingularityRequest(command, name, executor, resources, schedule, instances, daemon, env, uris, metadata, executorData, rackSensitive, id, version, timestamp);
  }
  
  public String getId() {
    return id;
  }

  public SingularityRequestBuilder setId(String id) {
    this.id = id;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public SingularityRequestBuilder setVersion(String version) {
    this.version = version;
    return this;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public SingularityRequestBuilder setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public SingularityRequestBuilder setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
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

  @Override
  public String toString() {
    return "SingularityRequestBuilder [id=" + id + ", name=" + name + ", version=" + version + ", timestamp=" + timestamp + ", metadata=" + metadata + ", executor=" + executor + ", resources=" + resources + ", schedule=" + schedule
        + ", instances=" + instances + ", rackSensitive=" + rackSensitive + ", daemon=" + daemon + ", command=" + command + ", env=" + env + ", uris=" + uris + ", executorData=" + executorData + "]";
  }
  
}
