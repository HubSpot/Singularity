package com.hubspot.singularity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hubspot.mesos.Resources;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingularityDeploy extends SingularityJsonObject {

  private final String requestId;
  
  private final String id;
  
  private final Optional<String> version;
  private final Optional<Long> timestamp;
  private final Optional<Map<String, String>> metadata;

  private final Optional<String> customExecutorCmd;
  private final Optional<String> customExecutorId;
  private final Optional<Resources> resources;
 
  private final Optional<String> command;
  private final Optional<Map<String, String>> env;
  private final Optional<List<String>> uris;
  private final Optional<Object> executorData;
  
  private final Optional<String> healthcheckUri;
  private final Optional<Long> healthcheckIntervalSeconds;
  private final Optional<Long> healthcheckTimeoutSeconds;
  
  private final Optional<Long> deployHealthTimeoutSeconds;

  private final Optional<String> loadBalancerBaseUri;
  private final Optional<List<String>> loadBalancerGroups;
  
  public static SingularityDeployBuilder newBuilder() {
    return new SingularityDeployBuilder();
  }
  
  public static SingularityDeploy fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityDeploy.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  @JsonCreator
  public SingularityDeploy(@JsonProperty("requestId") String requestId, @JsonProperty("id") String id, @JsonProperty("command") Optional<String> command, @JsonProperty("customExecutorCmd") Optional<String> customExecutorCmd,  @JsonProperty("customExecutorId") Optional<String> customExecutorId,
      @JsonProperty("resources") Optional<Resources> resources, @JsonProperty("env") Optional<Map<String, String>> env, @JsonProperty("uris") Optional<List<String>> uris, @JsonProperty("metadata") Optional<Map<String, String>> metadata,
      @JsonProperty("executorData") Optional<Object> executorData, @JsonProperty("version") Optional<String> version, @JsonProperty("timestamp") Optional<Long> timestamp, @JsonProperty("deployHealthTimeoutSeconds") Optional<Long> deployHealthTimeoutSeconds,
      @JsonProperty("healthcheckUri") Optional<String> healthcheckUri, @JsonProperty("healthcheckIntervalSeconds") Optional<Long> healthcheckIntervalSeconds, @JsonProperty("healthcheckTimeoutSeconds") Optional<Long> healthcheckTimeoutSeconds,
      @JsonProperty("loadBalancerBaseUri") Optional<String> loadBalancerBaseUri, @JsonProperty("loadBalancerGroups") Optional<List<String>> loadBalancerGroups) {
    this.requestId = requestId;
    
    this.command = command;
    this.resources = resources;
    
    this.customExecutorCmd = customExecutorCmd;
    this.customExecutorId = customExecutorId;
    
    this.metadata = metadata;
    this.version = version;
    this.id = id;
    this.timestamp = timestamp;
    this.env = env;
    this.uris = uris;
    this.executorData = executorData;
  
    this.healthcheckUri = healthcheckUri;
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
    this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
    
    this.deployHealthTimeoutSeconds = deployHealthTimeoutSeconds;

    this.loadBalancerBaseUri = loadBalancerBaseUri;
    this.loadBalancerGroups = loadBalancerGroups;
  }
  
  public SingularityDeployBuilder toBuilder() {
    return new SingularityDeployBuilder()
        .setCommand(command)
        .setRequestId(requestId)
        .setResources(resources)
        .setCustomExecutorCmd(customExecutorCmd)
        .setCustomExecutorId(customExecutorId)

        .setHealthcheckUri(healthcheckUri)
        .setHealthcheckIntervalSeconds(healthcheckIntervalSeconds)
        .setHealthcheckTimeoutSeconds(healthcheckTimeoutSeconds)
        
        .setMetadata(metadata.isPresent() ? Optional.<Map<String, String>> of(Maps.newHashMap(metadata.get())) : metadata)
        .setVersion(version)
        .setId(id)
        .setTimestamp(timestamp)
        .setEnv(env.isPresent() ? Optional.<Map<String, String>> of(Maps.newHashMap(env.get())) : env)
        .setUris(uris.isPresent() ? Optional.<List<String>> of(Lists.newArrayList(uris.get())) : uris)
        .setExecutorData(executorData);  // TODO: find the best way to clone this, maybe force it to be a Map<String, String> ?
  }  
  
  public Optional<Long> getDeployHealthTimeoutSeconds() {
    return deployHealthTimeoutSeconds;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getId() {
    return id;
  }

  public Optional<String> getVersion() {
    return version;
  }

  public Optional<Long> getTimestamp() {
    return timestamp;
  }

  public Optional<Map<String, String>> getMetadata() {
    return metadata;
  }

  public Optional<String> getCustomExecutorCmd() {
    return customExecutorCmd;
  }

  public Optional<String> getCustomExecutorId() {
    return customExecutorId;
  }

  public Optional<Resources> getResources() {
    return resources;
  }

  public Optional<String> getCommand() {
    return command;
  }

  public Optional<Map<String, String>> getEnv() {
    return env;
  }

  public Optional<List<String>> getUris() {
    return uris;
  }

  public Optional<Object> getExecutorData() {
    return executorData;
  }
  
  public Optional<String> getHealthcheckUri() {
    return healthcheckUri;
  }

  public Optional<Long> getHealthcheckIntervalSeconds() {
    return healthcheckIntervalSeconds;
  }

  public Optional<Long> getHealthcheckTimeoutSeconds() {
    return healthcheckTimeoutSeconds;
  }

  public Optional<String> getLoadBalancerBaseUri() {
    return loadBalancerBaseUri;
  }

  public Optional<List<String>> getLoadBalancerGroups() {
    return loadBalancerGroups;
  }

  @Override
  public String toString() {
    return "SingularityDeploy [requestId=" + requestId + ", id=" + id + ", version=" + version + ", timestamp=" + timestamp + ", metadata=" + metadata + ", customExecutorCmd=" + customExecutorCmd + ", customExecutorId=" + customExecutorId
        + ", resources=" + resources + ", command=" + command + ", env=" + env + ", uris=" + uris + ", executorData=" + executorData + ", healthcheckUri=" + healthcheckUri + ", healthcheckIntervalSeconds=" + healthcheckIntervalSeconds
        + ", healthcheckTimeoutSeconds=" + healthcheckTimeoutSeconds + ", deployHealthTimeoutSeconds=" + deployHealthTimeoutSeconds + ", loadBalancerBaseUri=" + loadBalancerBaseUri + ", loadBalancerGroups=" + loadBalancerGroups + "]";
  }

}
