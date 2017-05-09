package com.hubspot.singularity;

import java.util.List;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.TaskInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.mesos.MesosUtils;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityTask extends SingularityTaskIdHolder {

  private final SingularityTaskRequest taskRequest;
  private final Offer offer;
  private final TaskInfo mesosTask;
  private final Optional<String> rackId;

  @JsonCreator
  public SingularityTask(@JsonProperty("taskRequest") SingularityTaskRequest taskRequest, @JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("offer") Offer offer,
      @JsonProperty("mesosTask") TaskInfo task, @JsonProperty("rackId") Optional<String> rackId) {
    super(taskId);
    this.taskRequest = taskRequest;
    this.offer = offer;
    this.mesosTask = task;
    this.rackId = rackId;
  }

  public SingularityTaskRequest getTaskRequest() {
    return taskRequest;
  }

  @ApiModelProperty(hidden=true)
  public Offer getOffer() {
    return offer;
  }

  @ApiModelProperty(hidden=true)
  public TaskInfo getMesosTask() {
    return mesosTask;
  }

  public Optional<String> getRackId() {
    return rackId;
  }

  @JsonIgnore
  public Optional<Long> getPortByIndex(int index) {
    List<Long> ports = MesosUtils.getAllPorts(mesosTask.getResourcesList());
    if (index >= ports.size() || index < 0) {
      return Optional.absent();
    } else {
      return Optional.of(ports.get(index));
    }
  }

  @Override
  public String toString() {
    return "SingularityTask{" +
        "taskRequest=" + taskRequest +
        ", offer=" + MesosUtils.formatForLogging(offer) +
        ", mesosTask=" + MesosUtils.formatForLogging(mesosTask) +
        ", rackId=" + rackId +
        '}';
  }
}
