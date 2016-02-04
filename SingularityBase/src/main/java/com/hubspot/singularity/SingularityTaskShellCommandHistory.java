package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.JavaUtils;

public class SingularityTaskShellCommandHistory {

  private final SingularityTaskShellCommandRequest shellRequest;
  private final List<SingularityTaskShellCommandUpdate> shellUpdates;

  @JsonCreator
  public SingularityTaskShellCommandHistory(@JsonProperty("shellRequest") SingularityTaskShellCommandRequest shellRequest,
      @JsonProperty("shellUpdates") List<SingularityTaskShellCommandUpdate> shellUpdates) {
    this.shellRequest = shellRequest;
    this.shellUpdates = JavaUtils.nonNullImmutable(shellUpdates);
  }

  public SingularityTaskShellCommandRequest getShellRequest() {
    return shellRequest;
  }

  public List<SingularityTaskShellCommandUpdate> getShellUpdates() {
    return shellUpdates;
  }

  @Override
  public String toString() {
    return "SingularityTaskShellCommandHistory [shellRequest=" + shellRequest + ", shellUpdates=" + shellUpdates + "]";
  }

}
