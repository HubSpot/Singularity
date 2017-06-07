package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.TaskInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.hubspot.mesos.MesosUtils;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityTask extends SingularityTaskIdHolder {

  private final SingularityTaskRequest taskRequest;
  private final List<Offer> offers;
  private final TaskInfo mesosTask;
  private final Optional<String> rackId;

  @Deprecated
  public SingularityTask(SingularityTaskRequest taskRequest, SingularityTaskId taskId, Offer offer, TaskInfo task, Optional<String> rackId) {
    this(taskRequest, taskId, null, Collections.singletonList(offer), task, rackId);
  }

  public SingularityTask(SingularityTaskRequest taskRequest, SingularityTaskId taskId, List<Offer> offers, TaskInfo task, Optional<String> rackId) {
    this(taskRequest, taskId, null, offers, task, rackId);
  }

  @JsonCreator
  public SingularityTask(@JsonProperty("taskRequest") SingularityTaskRequest taskRequest, @JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("offer") Offer offer, @JsonProperty("offers") List<Offer> offers,
                         @JsonProperty("mesosTask") TaskInfo task, @JsonProperty("rackId") Optional<String> rackId) {
    super(taskId);
    Preconditions.checkArgument(offer != null || offers != null, "Must specify at least one of offer / offers");
    this.taskRequest = taskRequest;
    this.mesosTask = task;
    this.rackId = rackId;
    if (offers != null) {
      this.offers = offers;
    } else {
      this.offers = Collections.singletonList(offer);
    }
  }

  public SingularityTaskRequest getTaskRequest() {
    return taskRequest;
  }

  /*
   * Use getOffers instead. getOffer will currently return the first offer in getOffers
   */
  @Deprecated
  @ApiModelProperty(hidden=true)
  public Offer getOffer() {
    return offers.get(0);
  }

  @ApiModelProperty(hidden=true)
  public List<Offer> getOffers() {
    return offers;
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

  @JsonIgnore
  public SlaveID getSlaveId() {
    return offers.get(0).getSlaveId();
  }

  @JsonIgnore
  public String getHostname() {
    return offers.get(0).getHostname();
  }

  @Override
  public String toString() {
    return "SingularityTask{" +
        "taskRequest=" + taskRequest +
        ", offer=" + MesosUtils.formatForLogging(offers) +
        ", mesosTask=" + MesosUtils.formatForLogging(mesosTask) +
        ", rackId=" + rackId +
        '}';
  }
}
