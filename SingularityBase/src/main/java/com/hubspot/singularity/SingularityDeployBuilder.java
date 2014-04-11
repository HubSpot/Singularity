package com.hubspot.singularity;

import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.hubspot.mesos.Resources;

public class SingularityDeployBuilder {

  private String requestId;
  
  private String id;

  private Optional<String> version;
  private Optional<Long> timestamp;
  private Optional<Map<String, String>> metadata;

  private Optional<String> customExecutorCmd;
  private Optional<String> customExecutorId;
  private Optional<Resources> resources;
 
  private Optional<String> command;
  private Optional<Map<String, String>> env;
  private Optional<List<String>> uris;
  private Optional<Object> executorData;
 
  private Optional<String> healthcheckUri;
  private Optional<Long> healthcheckIntervalSeconds;
  private Optional<Long> healthcheckTimeoutSeconds;
  
  private Optional<Long> deployHealthTimeoutSeconds;
  
  public SingularityDeploy build() {
    return new SingularityDeploy(requestId, id, command, customExecutorCmd, customExecutorId, resources, env, uris, metadata, executorData, version, timestamp, deployHealthTimeoutSeconds, healthcheckUri, healthcheckIntervalSeconds, healthcheckTimeoutSeconds);
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

  public Optional<String> getVersion() {
    return version;
  }

  public SingularityDeployBuilder setVersion(Optional<String> version) {
    this.version = version;
    return this;
  }

  public Optional<Long> getTimestamp() {
    return timestamp;
  }

  public SingularityDeployBuilder setTimestamp(Optional<Long> timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public Optional<Map<String, String>> getMetadata() {
    return metadata;
  }

  public SingularityDeployBuilder setMetadata(Optional<Map<String, String>> metadata) {
    this.metadata = metadata;
    return this;
  }
    
  public Optional<String> getCustomExecutorCmd() {
    return customExecutorCmd;
  }

  public SingularityDeployBuilder setCustomExecutorCmd(Optional<String> customExecutorCmd) {
    this.customExecutorCmd = customExecutorCmd;
    return this;
  }

  public Optional<String> getCustomExecutorId() {
    return customExecutorId;
  }

  public SingularityDeployBuilder setCustomExecutorId(Optional<String> customExecutorId) {
    this.customExecutorId = customExecutorId;
    return this;
  }

  public Optional<Long> getDeployHealthTimeoutSeconds() {
    return deployHealthTimeoutSeconds;
  }

  public SingularityDeployBuilder setDeployHealthTimeoutSeconds(Optional<Long> deployHealthTimeoutSeconds) {
    this.deployHealthTimeoutSeconds = deployHealthTimeoutSeconds;
    return this;
  }

  public Optional<Resources> getResources() {
    return resources;
  }

  public SingularityDeployBuilder setResources(Optional<Resources> resources) {
    this.resources = resources;
    return this;
  }

  public Optional<String> getCommand() {
    return command;
  }

  public SingularityDeployBuilder setCommand(Optional<String> command) {
    this.command = command;
    return this;
  }

  public Optional<Map<String, String>> getEnv() {
    return env;
  }

  public SingularityDeployBuilder setEnv(Optional<Map<String, String>> env) {
    this.env = env;
    return this;
  }

  public Optional<List<String>> getUris() {
    return uris;
  }

  public SingularityDeployBuilder setUris(Optional<List<String>> uris) {
    this.uris = uris;
    return this;
  }

  public Optional<Object> getExecutorData() {
    return executorData;
  }

  public SingularityDeployBuilder setExecutorData(Optional<Object> executorData) {
    this.executorData = executorData;
    return this;
  }

  public Optional<String> getHealthcheckUri() {
    return healthcheckUri;
  }

  public SingularityDeployBuilder setHealthcheckUri(Optional<String> healthcheckUri) {
    this.healthcheckUri = healthcheckUri;
    return this;
  }

  public Optional<Long> getHealthcheckIntervalSeconds() {
    return healthcheckIntervalSeconds;
  }

  public SingularityDeployBuilder setHealthcheckIntervalSeconds(Optional<Long> healthcheckIntervalSeconds) {
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
    return this;
  }

  public Optional<Long> getHealthcheckTimeoutSeconds() {
    return healthcheckTimeoutSeconds;
  }

  public SingularityDeployBuilder setHealthcheckTimeoutSeconds(Optional<Long> healthcheckTimeoutSeconds) {
    this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
    return this;
  }

  @Override
  public String toString() {
    return "SingularityDeployBuilder [requestId=" + requestId + ", id=" + id + ", version=" + version + ", timestamp=" + timestamp + ", metadata=" + metadata + ", customExecutorCmd=" + customExecutorCmd + ", customExecutorId="
        + customExecutorId + ", resources=" + resources + ", command=" + command + ", env=" + env + ", uris=" + uris + ", executorData=" + executorData + ", healthcheckUri=" + healthcheckUri + ", healthcheckIntervalSeconds="
        + healthcheckIntervalSeconds + ", healthcheckTimeoutSeconds=" + healthcheckTimeoutSeconds + ", deployHealthTimeoutSeconds=" + deployHealthTimeoutSeconds + "]";
  }
  
}
