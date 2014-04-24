package com.hubspot.deploy;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutorData {
  
  private final String cmd;
  private final List<EmbeddedArtifact> embeddedArtifacts;
  private final List<ExternalArtifact> externalArtifacts;
  private final List<Integer> successfulExitCodes;
  private final Optional<String> runningSentinel;
  private final Optional<String> user;
  private final List<String> extraCmdLineArgs;
  
  @JsonCreator
  public static ExecutorData fromString(String value) {
    return new ExecutorData(value, Collections.<EmbeddedArtifact> emptyList(), Collections.<ExternalArtifact> emptyList(), null, null, null, Collections.<String> emptyList());
  }

  @JsonCreator
  public ExecutorData(@JsonProperty("cmd") String cmd, @JsonProperty("embeddedArtifacts") List<EmbeddedArtifact> embeddedArtifacts, @JsonProperty("externalArtifacts") List<ExternalArtifact> externalArtifacts, 
      @JsonProperty("successfulExitCodes") List<Integer> successfulExitCodes, @JsonProperty("user") Optional<String> user, @JsonProperty("runningSentinel") Optional<String> runningSentinel, 
      @JsonProperty("extraCmdLineArgs") List<String> extraCmdLineArgs) {
    this.cmd = cmd;
    this.embeddedArtifacts = nonNullImmutable(embeddedArtifacts);
    this.externalArtifacts = nonNullImmutable(externalArtifacts);
    this.user = user;
    this.successfulExitCodes = nonNullImmutable(successfulExitCodes);
    this.extraCmdLineArgs = nonNullImmutable(extraCmdLineArgs);
    this.runningSentinel = runningSentinel;
  }
  
  public ExecutorDataBuilder toBuilder() {
    return new ExecutorDataBuilder(cmd, embeddedArtifacts, externalArtifacts, successfulExitCodes, runningSentinel, user, extraCmdLineArgs);
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
        .toString();
  }
}
