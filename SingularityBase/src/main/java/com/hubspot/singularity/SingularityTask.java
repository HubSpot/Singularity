package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.TaskInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.json.SingularityMesosOfferObject;
import com.hubspot.mesos.json.SingularityMesosTaskObject;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityTask extends SingularityTaskIdHolder {

  private final SingularityTaskRequest taskRequest;
  private final List<SingularityMesosOfferObject> offers;
  /*
   * Only used when we first send this task to mesos.  This should not be saved to JSON
   * A subset of this information which we have greater control over is saved in the
   * SingularityMesosTaskObject
   */
  private final TaskInfo mesosTaskProtos;
  private final SingularityMesosTaskObject mesosTask;
  private final Optional<String> rackId;

  public SingularityTask(SingularityTaskRequest taskRequest, SingularityTaskId taskId, List<SingularityMesosOfferObject> offers, TaskInfo mesosTaskProtos, SingularityMesosTaskObject task, Optional<String> rackId) {
    this(taskRequest, taskId, null, offers, mesosTaskProtos, task, rackId);
  }

  public SingularityTask(SingularityTaskRequest taskRequest, SingularityTaskId taskId, List<SingularityMesosOfferObject> offers, SingularityMesosTaskObject task, Optional<String> rackId) {
    this(taskRequest, taskId, null, offers, null, task, rackId);
  }

  @JsonCreator
  public SingularityTask(@JsonProperty("taskRequest") SingularityTaskRequest taskRequest, @JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("offer") SingularityMesosOfferObject offer, @JsonProperty("offers") List<SingularityMesosOfferObject> offers,
                         @JsonProperty("mesosTaskProtos") TaskInfo mesosTaskProtos, @JsonProperty("mesosTask") SingularityMesosTaskObject task, @JsonProperty("rackId") Optional<String> rackId) {
    super(taskId);
    Preconditions.checkArgument(offer != null || offers != null, "Must specify at least one of offer / offers");
    this.taskRequest = taskRequest;
    this.mesosTask = task;
    this.mesosTaskProtos = mesosTaskProtos;
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
  public SingularityMesosOfferObject getOffer() {
    return offers.get(0);
  }

  @ApiModelProperty(hidden=true)
  public List<SingularityMesosOfferObject> getOffers() {
    return offers;
  }

  @ApiModelProperty(hidden=true)
  public SingularityMesosTaskObject getMesosTask() {
    return mesosTask;
  }

  @JsonIgnore
  public TaskInfo getMesosTaskProtos() {
    return mesosTaskProtos;
  }

  public Optional<String> getRackId() {
    return rackId;
  }

  @JsonIgnore
  public Optional<Long> getPortByIndex(int index) {
    List<Long> ports = MesosUtils.getAllPorts(mesosTask.getResources());
    if (index >= ports.size() || index < 0) {
      return Optional.absent();
    } else {
      return Optional.of(ports.get(index));
    }
  }

  @JsonIgnore
  public AgentID getAgentId() {
    return offers.get(0).getAgentId();
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
