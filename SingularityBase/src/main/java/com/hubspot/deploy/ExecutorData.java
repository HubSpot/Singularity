package com.hubspot.deploy;

import java.util.Collection;
import java.util.Collections;
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
  private final Collection<String> uris;
  private final Map<String, String> env;
  private final Optional<ArtifactInfo> artifact;
  private final Optional<ArtifactInfo> deployConfig;
  private final Collection<Integer> exitCodes;
  private final Optional<String> runningSentinel;

  @JsonCreator
  public static ExecutorData fromString(String value) {
    return new ExecutorData(value, Collections.<String>emptyList(), Collections.<String, String>emptyMap(), null, null, null, null);
  }

  @JsonCreator
  public ExecutorData(@JsonProperty("cmd") String cmd, @JsonProperty("uris") Collection<String> uris,
                             @JsonProperty("env") Map<String, String> env,
                             @JsonProperty("artifact") ArtifactInfo artifact,
                             @JsonProperty("deployConfig") ArtifactInfo deployConfig,
                             @JsonProperty("exitCodes") Collection<Integer> exitCodes,
                             @JsonProperty("runningSentinel") String runningSentinel) {
    this.cmd = cmd;
    this.uris = Objects.firstNonNull(uris, Collections.<String>emptyList());
    this.env = Objects.firstNonNull(env, Collections.<String, String>emptyMap());
    this.artifact = Optional.fromNullable(artifact);
    this.deployConfig = Optional.fromNullable(deployConfig);
    this.exitCodes = Objects.firstNonNull(exitCodes, Collections.singletonList(0));

    this.runningSentinel = Strings.isNullOrEmpty(runningSentinel) ? Optional.<String>absent() : Optional.of(runningSentinel);
  }

  public String getCmd() {
    return cmd;
  }

  public Collection<String> getUris() {
    return uris;
  }

  public Map<String, String> getEnv() {
    return env;
  }

  public Optional<ArtifactInfo> getArtifact() {
    return artifact;
  }

  public Optional<ArtifactInfo> getDeployConfig() {
    return deployConfig;
  }

  public Collection<Integer> getExitCodes() {
    return exitCodes;
  }

  public Optional<String> getRunningSentinel() {
    return runningSentinel;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("cmd", cmd)
        .add("uris", uris)
        .add("env", env)
        .add("artifact", artifact)
        .add("deployConfig", deployConfig)
        .add("exitCodes", exitCodes)
        .add("runningSentinel", runningSentinel)
        .toString();
  }
}
