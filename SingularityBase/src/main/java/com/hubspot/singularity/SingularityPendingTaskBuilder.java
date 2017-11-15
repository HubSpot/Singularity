package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.hubspot.mesos.Resources;

public class SingularityPendingTaskBuilder {

  private SingularityPendingTaskId pendingTaskId;
  private Optional<List<String>> cmdLineArgsList;
  private Optional<String> user;
  private Optional<String> runId;
  private Optional<Boolean> skipHealthchecks;
  private Optional<String> message;
  private Optional<Resources> resources;
  private Map<String, String> envOverrides;
  private Optional<String> actionId;

  public SingularityPendingTaskBuilder() {
    this.pendingTaskId = null;
    this.cmdLineArgsList = Optional.absent();
    this.user = Optional.absent();
    this.runId = Optional.absent();
    this.skipHealthchecks = Optional.absent();
    this.message = Optional.absent();
    this.resources = Optional.absent();
    this.envOverrides = Collections.emptyMap();
    this.actionId = Optional.absent();
  }

  public SingularityPendingTaskBuilder setPendingTaskId(SingularityPendingTaskId pendingTaskId) {
    this.pendingTaskId = pendingTaskId;
    return this;
  }


  public SingularityPendingTaskBuilder setCmdLineArgsList(List<String> cmdLineArgsList) {
    this.cmdLineArgsList = Optional.of(cmdLineArgsList);
    return this;
  }

  public SingularityPendingTaskBuilder setCmdLineArgsList(Optional<List<String>> cmdLineArgsList) {
    this.cmdLineArgsList = cmdLineArgsList;
    return this;
  }

  public SingularityPendingTaskBuilder setUser(String user) {
    this.user = Optional.of(user);
    return this;
  }

  public SingularityPendingTaskBuilder setRunId(String runId) {
    this.runId = Optional.of(runId);
    return this;
  }

  public SingularityPendingTaskBuilder setRunId(Optional<String> runId) {
    this.runId = runId;
    return this;
  }

  public SingularityPendingTaskBuilder setSkipHealthchecks(Boolean skipHealthchecks) {
    this.skipHealthchecks = Optional.of(skipHealthchecks);
    return this;
  }

  public SingularityPendingTaskBuilder setMessage(String message) {
    this.message = Optional.of(message);
    return this;
  }

  public SingularityPendingTaskBuilder setResources(Resources resources) {
    this.resources = Optional.of(resources);
    return this;
  }

  public SingularityPendingTaskBuilder setEnvOverrides(Map<String, String> envOverrides) {
    this.envOverrides = envOverrides;
    return this;
  }
  public SingularityPendingTaskBuilder setActionId(String actionId) {
    this.actionId = Optional.of(actionId);
    return this;
  }

  public SingularityPendingTask build() {
    return new SingularityPendingTask(
        pendingTaskId, cmdLineArgsList, user, runId, skipHealthchecks, message, resources, envOverrides, actionId
    );
  }

  @Override
  public String toString() {
    return "SingularityPendingTask{" +
        "pendingTaskId=" + pendingTaskId +
        ", cmdLineArgsList=" + cmdLineArgsList +
        ", user=" + user +
        ", runId=" + runId +
        ", skipHealthchecks=" + skipHealthchecks +
        ", message=" + message +
        ", resources=" + resources +
        ", envOverrides=" + envOverrides +
        ", actionId=" + actionId +
        '}';
  }
}
