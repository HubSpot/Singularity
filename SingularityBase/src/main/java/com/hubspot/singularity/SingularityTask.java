package com.hubspot.singularity;

import java.io.IOException;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.TaskInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityTask extends SingularityJsonObject {

  private final SingularityTaskRequest taskRequest;
  private final SingularityTaskId taskId;
  private final Offer offer;
  private final TaskInfo mesosTask;

  @JsonCreator
  public SingularityTask(@JsonProperty("taskRequest") SingularityTaskRequest taskRequest, @JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("offer") Offer offer, @JsonProperty("mesosTask") TaskInfo task) {
    this.taskRequest = taskRequest;
    this.offer = offer;
    this.mesosTask = task;
    this.taskId = taskId;
  }

  public SingularityTaskId getTaskId() {
    return taskId;
  }

  public SingularityTaskRequest getTaskRequest() {
    return taskRequest;
  }

  public Offer getOffer() {
    return offer;
  }

  public TaskInfo getMesosTask() {
    return mesosTask;
  }

  public static SingularityTask fromBytes(byte[] bytes, ObjectMapper objectMapper) throws JsonParseException, JsonMappingException, IOException {
    return objectMapper.readValue(bytes, SingularityTask.class);
  }

  @Override
  public String toString() {
    return "SingularityTask [taskRequest=" + taskRequest + ", taskId=" + taskId + ", offer=" + offer + ", task=" + mesosTask + "]";
  }
  
}
