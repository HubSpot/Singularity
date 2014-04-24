package com.hubspot.deploy;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class ExecutorDataBuilder {
  
  private String cmd;
  private List<EmbeddedArtifact> embeddedArtifacts;
  private List<ExternalArtifact> externalArtifacts;
  private List<Integer> successfulExitCodes;
  private Optional<String> runningSentinel;
  private Optional<String> user;
  private List<String> extraCmdLineArgs;
  
  public ExecutorDataBuilder(String cmd) {
    this(cmd, Lists.<EmbeddedArtifact> newArrayList(), Lists.<ExternalArtifact> newArrayList(), Lists.<Integer> newArrayList(), Optional.<String> absent(), Optional.<String> absent(), Lists.<String> newArrayList());
  }
  
  public ExecutorDataBuilder(String cmd, List<EmbeddedArtifact> embeddedArtifacts, List<ExternalArtifact> externalArtifacts, List<Integer> successfulExitCodes, Optional<String> runningSentinel,
      Optional<String> user, List<String> extraCmdLineArgs) {
    this.cmd = cmd;
    this.embeddedArtifacts = embeddedArtifacts;
    this.externalArtifacts = externalArtifacts;
    this.successfulExitCodes = successfulExitCodes;
    this.runningSentinel = runningSentinel;
    this.user = user;
    this.extraCmdLineArgs = extraCmdLineArgs;
  }
  
  public ExecutorData build() {
    return new ExecutorData(cmd, embeddedArtifacts, externalArtifacts, successfulExitCodes, user, runningSentinel, extraCmdLineArgs);
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

  public ExecutorDataBuilder setCmd(String cmd) {
    this.cmd = cmd;
    return this;
  }

  public ExecutorDataBuilder setEmbeddedArtifacts(List<EmbeddedArtifact> embeddedArtifacts) {
    this.embeddedArtifacts = embeddedArtifacts;
    return this;
  }

  public ExecutorDataBuilder setExternalArtifacts(List<ExternalArtifact> externalArtifacts) {
    this.externalArtifacts = externalArtifacts;
    return this;
  }

  public ExecutorDataBuilder setSuccessfulExitCodes(List<Integer> successfulExitCodes) {
    this.successfulExitCodes = successfulExitCodes;
    return this;
  }

  public ExecutorDataBuilder setRunningSentinel(Optional<String> runningSentinel) {
    this.runningSentinel = runningSentinel;
    return this;
  }

  public ExecutorDataBuilder setUser(Optional<String> user) {
    this.user = user;
    return this;
  }

  public ExecutorDataBuilder setExtraCmdLineArgs(List<String> extraCmdLineArgs) {
    this.extraCmdLineArgs = extraCmdLineArgs;
    return this;
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
