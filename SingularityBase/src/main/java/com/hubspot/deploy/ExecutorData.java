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
  private final Collection<Artifact> artifacts;
  private final Map<String, String> env;
  private final Collection<Integer> exitCodes;
  private final Optional<String> runningSentinel;
  private final Optional<String> user;

  @JsonCreator
  public static ExecutorData fromString(String value) {
    return new ExecutorData(value, Collections.<Artifact> emptyList(), Collections.<String, String> emptyMap(), null, null, null);
  }

  @JsonCreator
  public ExecutorData(@JsonProperty("cmd") String cmd, @JsonProperty("artifacts") Collection<Artifact> artifacts, @JsonProperty("env") Map<String, String> env, 
      @JsonProperty("exitCodes") Collection<Integer> exitCodes, @JsonProperty("user") String user, @JsonProperty("runningSentinel") String runningSentinel) {
    this.cmd = cmd;
    this.artifacts = Objects.firstNonNull(artifacts, Collections.<Artifact>emptyList());
    this.env = Objects.firstNonNull(env, Collections.<String, String>emptyMap());
    this.user = Optional.fromNullable(user);
    this.exitCodes = Objects.firstNonNull(exitCodes, Collections.singletonList(0));

    this.runningSentinel = Strings.isNullOrEmpty(runningSentinel) ? Optional.<String> absent() : Optional.of(runningSentinel);
  }

  public String getCmd() {
    return cmd;
  }

  public Collection<Artifact> getArtifacts() {
    return artifacts;
  }

  public Map<String, String> getEnv() {
    return env;
  }

  public Collection<Integer> getExitCodes() {
    return exitCodes;
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
        .add("artifacts", artifacts)
        .add("env", env)
        .add("user", user)
        .add("exitCodes", exitCodes)
        .add("runningSentinel", runningSentinel)
        .toString();
  }
}
