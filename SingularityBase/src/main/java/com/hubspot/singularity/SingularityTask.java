package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hubspot.mesos.protos.MesosRangeObject;
import com.hubspot.mesos.protos.MesosResourceObject;
import com.hubspot.mesos.protos.MesosStringValue;
import com.hubspot.mesos.protos.MesosOfferObject;
import com.hubspot.mesos.protos.MesosTaskObject;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityTask extends SingularityTaskIdHolder {

  private final SingularityTaskRequest taskRequest;
  private final List<MesosOfferObject> offers;
  private final MesosTaskObject mesosTask;
  private final Optional<String> rackId;

  public SingularityTask(SingularityTaskRequest taskRequest, SingularityTaskId taskId, List<MesosOfferObject> offers, MesosTaskObject task, Optional<String> rackId) {
    this(taskRequest, taskId, null, offers, task, rackId);
  }

  @JsonCreator
  public SingularityTask(@JsonProperty("taskRequest") SingularityTaskRequest taskRequest,
                         @JsonProperty("taskId") SingularityTaskId taskId,
                         @JsonProperty("offer") MesosOfferObject offer,
                         @JsonProperty("offers") List<MesosOfferObject> offers,
                         @JsonProperty("mesosTask") MesosTaskObject task,
                         @JsonProperty("rackId") Optional<String> rackId) {
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
  public MesosOfferObject getOffer() {
    return offers.get(0);
  }

  @ApiModelProperty(hidden=true)
  public List<MesosOfferObject> getOffers() {
    return offers;
  }

  @ApiModelProperty(hidden=true)
  public MesosTaskObject getMesosTask() {
    return mesosTask;
  }

  public Optional<String> getRackId() {
    return rackId;
  }

  @JsonIgnore
  public MesosStringValue getAgentId() {
    return offers.get(0).getAgentId();
  }

  @JsonIgnore
  public String getHostname() {
    return offers.get(0).getHostname();
  }

  @JsonIgnore
  public Optional<Long> getPortByIndex(int index) {
    Optional<MesosResourceObject> maybePortResource = getPortsResource();
    final List<Long> ports = Lists.newArrayList();
    if (maybePortResource.isPresent() && maybePortResource.get().hasRanges()) {
      List<MesosRangeObject> portRanges = maybePortResource.get().getRanges().getRangesList();
      for (MesosRangeObject range : portRanges) {
        for (long port = range.getBegin(); port <= range.getEnd(); port++) {
          ports.add(port);
        }
      }
    }
    if (index >= ports.size() || index < 0) {
      return Optional.absent();
    } else {
      Collections.sort(ports); // ports are always in ascending order
      return Optional.of(ports.get(index));
    }
  }

  @JsonIgnore
  private Optional<MesosResourceObject> getPortsResource() {
    for (MesosResourceObject resourceObject : mesosTask.getResources()) {
      if (resourceObject.hasName() && resourceObject.getName().equals("ports")) {
        return Optional.of(resourceObject);
      }
    }
    return Optional.absent();
  }

  @Override
  public String toString() {
    return "SingularityTask{" +
        "taskRequest=" + taskRequest +
        ", offer=" + offers +
        ", mesosTask=" + mesosTask +
        ", rackId=" + rackId +
        '}';
  }
}
