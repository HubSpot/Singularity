package com.hubspot.deploy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutorData {
  
  private final String cmd;
  private final List<EmbeddedArtifact> embeddedArtifacts;
  private final List<ExternalArtifact> externalArtifacts;
  private final Map<String, String> env;
  private final List<Integer> successfulExitCodes;
  private final Optional<String> runningSentinel;
  private final Optional<String> user;
  private final List<String> extraCmdLineArgs;
  
  @JsonCreator
  public static ExecutorData fromString(String value) {
    return new ExecutorData(value, Collections.<EmbeddedArtifact> emptyList(), Collections.<ExternalArtifact> emptyList(), Collections.<String, String> emptyMap(), null, null, null, Collections.<String> emptyList());
  }

  @JsonCreator
  public ExecutorData(@JsonProperty("cmd") String cmd, @JsonProperty("embeddedArtifacts") List<EmbeddedArtifact> embeddedArtifacts, @JsonProperty("externalArtifacts") List<ExternalArtifact> externalArtifacts, 
      @JsonProperty("env") Map<String, String> env, @JsonProperty("successfulExitCodes") List<Integer> successfulExitCodes, @JsonProperty("user") String user, @JsonProperty("runningSentinel") String runningSentinel, 
      @JsonProperty("extraCmdLineArgs") List<String> extraCmdLineArgs) {
    this.cmd = cmd;
    this.embeddedArtifacts = Objects.firstNonNull(embeddedArtifacts, Collections.<EmbeddedArtifact> emptyList());
    this.externalArtifacts = Objects.firstNonNull(externalArtifacts, Collections.<ExternalArtifact> emptyList());
    this.env = Objects.firstNonNull(env, Collections.<String, String> emptyMap());
    this.user = Optional.fromNullable(user);
    this.successfulExitCodes = Objects.firstNonNull(successfulExitCodes, Collections.singletonList(0));
    this.extraCmdLineArgs = Objects.firstNonNull(extraCmdLineArgs, Collections.<String> emptyList());
    
    this.runningSentinel = Strings.isNullOrEmpty(runningSentinel) ? Optional.<String> absent() : Optional.of(runningSentinel);
  }

  public String getCmd() {
    return cmd;
  }

  public Map<String, String> getEnv() {
    return env;
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
        .add("env", env)
        .add("user", user)
        .add("successfulExitCodes", successfulExitCodes)
        .add("runningSentinel", runningSentinel)
        .add("extraCmdLineArgs", extraCmdLineArgs)
        .toString();
  }
}
