package com.hubspot.singularity;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hubspot.mesos.Resources;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingularityDeploy extends SingularityJsonObject {

  private final String requestId;
  
  private final String id;

  private final String version;
  private final Long timestamp;
  private final Map<String, String> metadata;

  private final String executor;
  private final Resources resources;
 
  private final String command;
  private final Map<String, String> env;
  private final List<String> uris;
  private final Object executorData;
  
  public static SingularityDeployBuilder newBuilder() {
    return new SingularityDeployBuilder();
  }
  
  public static SingularityDeploy fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityDeploy.class);
  }

  @JsonCreator
  public SingularityDeploy(@JsonProperty("requestId") String requestId, @JsonProperty("command") String command, @JsonProperty("executor") String executor, @JsonProperty("resources") Resources resources,
      @JsonProperty("env") Map<String, String> env, @JsonProperty("uris") List<String> uris, @JsonProperty("metadata") Map<String, String> metadata,
      @JsonProperty("executorData") Object executorData, @JsonProperty("id") String id, @JsonProperty("version") String version, @JsonProperty("timestamp") Long timestamp) {
    this.requestId = requestId;
    
    this.command = command;
    this.resources = resources;
    this.executor = executor;

    this.metadata = metadata;
    this.version = version;
    this.id = id;
    this.timestamp = timestamp;
    this.env = env;
    this.uris = uris;
    this.executorData = executorData;
  }
  
  public SingularityDeployBuilder toBuilder() {
    return new SingularityDeployBuilder()
        .setCommand(command)
        .setRequestId(requestId)
        .setResources(resources)
        .setExecutor(executor)

        .setMetadata(metadata == null ? null : Maps.newHashMap(metadata))
        .setVersion(version)
        .setId(id)
        .setTimestamp(timestamp)
        .setEnv(env == null ? null : Maps.newHashMap(env))
        .setUris(uris == null ? null : Lists.newArrayList(uris))
        .setExecutorData(executorData);  // TODO: find the best way to clone this, maybe force it to be a Map<String, String> ?
  }

  public String getId() {
    return id;
  }

  public String getVersion() {
    return version;
  }

  public String getRequestId() {
    return requestId;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public Map<String, String> getEnv() {
    return env;
  }

  public List<String> getUris() {
    return uris;
  }

  public Object getExecutorData() {
    return executorData;
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
    return "SingularityDeploy [requestId=" + requestId + ", id=" + id + ", version=" + version + ", timestamp=" + timestamp + ", metadata=" + metadata + ", executor=" + executor + ", resources=" + resources + ", command=" + command
        + ", env=" + env + ", uris=" + uris + ", executorData=" + executorData + "]";
  }

}
