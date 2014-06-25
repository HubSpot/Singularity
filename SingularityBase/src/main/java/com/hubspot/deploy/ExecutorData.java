package com.hubspot.deploy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutorData {
  
  private final String cmd;
  private final List<EmbeddedArtifact> embeddedArtifacts;
  private final List<ExternalArtifact> externalArtifacts;
  private final List<Integer> successfulExitCodes;
  private final Optional<String> runningSentinel;
  private final Optional<String> user;
  private final List<String> extraCmdLineArgs;
  private final Optional<String> loggingTag;
  private final Map<String, String> loggingExtraFields;
  private final Optional<Long> sigKillProcessesAfterMillis;
  
  @JsonCreator
  public ExecutorData(@JsonProperty("cmd") String cmd, @JsonProperty("embeddedArtifacts") List<EmbeddedArtifact> embeddedArtifacts, @JsonProperty("externalArtifacts") List<ExternalArtifact> externalArtifacts, 
      @JsonProperty("successfulExitCodes") List<Integer> successfulExitCodes, @JsonProperty("user") Optional<String> user, @JsonProperty("runningSentinel") Optional<String> runningSentinel, 
      @JsonProperty("extraCmdLineArgs") List<String> extraCmdLineArgs, @JsonProperty("loggingTag") Optional<String> loggingTag, @JsonProperty("loggingExtraFields") Map<String, String> loggingExtraFields, 
      @JsonProperty("sigKillProcessesAfterMillis") Optional<Long> sigKillProcessesAfterMillis) {
    this.cmd = cmd;
    this.embeddedArtifacts = nonNullImmutable(embeddedArtifacts);
    this.externalArtifacts = nonNullImmutable(externalArtifacts);
    this.user = user;
    this.successfulExitCodes = nonNullImmutable(successfulExitCodes);
    this.extraCmdLineArgs = nonNullImmutable(extraCmdLineArgs);
    this.runningSentinel = runningSentinel;
    this.loggingTag = loggingTag;
    this.loggingExtraFields = nonNullImmutable(loggingExtraFields);
    this.sigKillProcessesAfterMillis = sigKillProcessesAfterMillis;
  }
  
  public ExecutorDataBuilder toBuilder() {
    return new ExecutorDataBuilder(cmd, embeddedArtifacts, externalArtifacts, successfulExitCodes, runningSentinel, user, extraCmdLineArgs, loggingTag, loggingExtraFields, sigKillProcessesAfterMillis);
  }
  
  private <K, V> Map<K, V> nonNullImmutable(Map<K, V> map) {
    if (map == null) {
      return Collections.emptyMap();
    }
    return ImmutableMap.copyOf(map);
  }
  
  private <T> List<T> nonNullImmutable(List<T> list) {
    if (list == null) {
      return Collections.emptyList();
    }
    return ImmutableList.copyOf(list);
  }

  public String getCmd() {
    return cmd;
  }

  public Optional<String> getLoggingTag() {
    return loggingTag;
  }

  public Map<String, String> getLoggingExtraFields() {
    return loggingExtraFields;
  }

  public List<EmbeddedArtifact> getEmbeddedArtifacts() {
    return embeddedArtifacts;
  }

  public List<ExternalArtifact> getExternalArtifacts() {
    return externalArtifacts;
  }

  public List<Integer> getSuccessfulExitCodes() {
    return successfulExitCodes;
  }

  public List<String> getExtraCmdLineArgs() {
    return extraCmdLineArgs;
  }

  public Optional<String> getRunningSentinel() {
    return runningSentinel;
  }

  public Optional<String> getUser() {
    return user;
  }
 
  public Optional<Long> getSigKillProcessesAfterMillis() {
    return sigKillProcessesAfterMillis;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("cmd", cmd)
        .add("embeddedArtifacts", embeddedArtifacts)
        .add("externalArtifacts", externalArtifacts)
        .add("user", user)
        .add("successfulExitCodes", successfulExitCodes)
        .add("runningSentinel", runningSentinel)
        .add("extraCmdLineArgs", extraCmdLineArgs)
        .add("loggingTag", loggingTag)
        .add("loggingExtraFields", loggingExtraFields)
        .add("sigKillProcessesAfterMillis", sigKillProcessesAfterMillis)
        .toString();
  }
}
