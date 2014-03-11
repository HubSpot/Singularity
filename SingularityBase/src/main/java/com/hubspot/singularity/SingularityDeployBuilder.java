package com.hubspot.singularity;

import java.util.List;
import java.util.Map;

import com.hubspot.mesos.Resources;

public class SingularityDeployBuilder {

  private String requestId;
  
  private String id;

  private String version;
  private Long timestamp;
  private Map<String, String> metadata;

  private String executor;
  private Resources resources;
  
  private String command;
  private Map<String, String> env;
  private List<String> uris;
  private Object executorData;

  public SingularityDeploy build() {
    return new SingularityDeploy(requestId, command, executor, resources, env, uris, metadata, executorData, id, version, timestamp);
  }

  public String getRequestId() {
    return requestId;
  }

  public SingularityDeployBuilder setRequestId(String requestId) {
    this.requestId = requestId;
    return this;
  }

  public String getId() {
    return id;
  }

  public SingularityDeployBuilder setId(String id) {
    this.id = id;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public SingularityDeployBuilder setVersion(String version) {
    this.version = version;
    return this;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public SingularityDeployBuilder setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public SingularityDeployBuilder setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }
  
  public String getExecutor() {
    return executor;
  }

  public SingularityDeployBuilder setExecutor(String executor) {
    this.executor = executor;
    return this;
  }

  public Resources getResources() {
    return resources;
  }

  public SingularityDeployBuilder setResources(Resources resources) {
    this.resources = resources;
    return this;
  }
  
  public String getCommand() {
    return command;
  }

  public SingularityDeployBuilder setCommand(String command) {
    this.command = command;
    return this;
  }

  public Map<String, String> getEnv() {
    return env;
  }

  public SingularityDeployBuilder setEnv(Map<String, String> env) {
    this.env = env;
    return this;
  }

  public List<String> getUris() {
    return uris;
  }

  public SingularityDeployBuilder setUris(List<String> uris) {
    this.uris = uris;
    return this;
  }
  
  public Object getExecutorData() {
    return executorData;
  }

  public SingularityDeployBuilder setExecutorData(Object executorData) {
    this.executorData = executorData;
    return this;
  }

  @Override
  public String toString() {
    return "SingularityDeployBuilder [requestId=" + requestId + ", id=" + id + ", version=" + version + ", timestamp=" + timestamp + ", metadata=" + metadata + ", executor=" + executor + ", resources=" + resources + ", command=" + command
        + ", env=" + env + ", uris=" + uris + ", executorData=" + executorData + "]";
  }
  
}
