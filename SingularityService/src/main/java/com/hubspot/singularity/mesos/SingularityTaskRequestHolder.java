package com.hubspot.singularity.mesos;

import java.util.ArrayList;
import java.util.List;

import com.hubspot.mesos.Resources;
import com.hubspot.singularity.SingularityTaskRequest;

public class SingularityTaskRequestHolder {

  private final SingularityTaskRequest taskRequest;
  private final Resources executorResources;
  private final Resources taskResources;
  private final Resources totalResources;
  private final List<Long> requestedPorts;

  public SingularityTaskRequestHolder(SingularityTaskRequest taskRequest, Resources defaultResources, Resources defaultCustomExecutorResources) {
    this.taskRequest = taskRequest;
    this.executorResources = taskRequest.getDeploy().getCustomExecutorCmd().isPresent() ?
        taskRequest.getDeploy().getCustomExecutorResources().or(defaultCustomExecutorResources) : Resources.EMPTY_RESOURCES;;
    this.taskResources = taskRequest.getPendingTask().getResources().or(taskRequest.getDeploy().getResources()).or(defaultResources);
    this.totalResources = Resources.add(taskResources, executorResources);
    this.requestedPorts = new ArrayList<>();
    if (taskRequest.getDeploy().getContainerInfo().isPresent() && taskRequest.getDeploy().getContainerInfo().get().getDocker().isPresent()) {
      requestedPorts.addAll(taskRequest.getDeploy().getContainerInfo().get().getDocker().get().getLiteralHostPorts());
    }
  }

  public SingularityTaskRequest getTaskRequest() {
    return taskRequest;
  }

  public Resources getExecutorResources() {
    return executorResources;
  }

  public Resources getTaskResources() {
    return taskResources;
  }

  public Resources getTotalResources() {
    return totalResources;
  }

  public List<Long> getRequestedPorts() {
    return requestedPorts;
  }

  @Override
  public String toString() {
    return "SingularityTaskRequestHolder [" + (taskRequest != null ? "taskRequest=" + taskRequest + ", " : "") + (executorResources != null ? "executorResources=" + executorResources + ", " : "")
        + (taskResources != null ? "taskResources=" + taskResources + ", " : "") + (totalResources != null ? "totalResources=" + totalResources + ", " : "")
        + (requestedPorts != null ? "requestedPorts=" + requestedPorts : "") + "]";
  }

}
