package com.hubspot.singularity;

import java.io.IOException;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.TaskInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityTask {

  private final SingularityTaskRequest taskRequest;
  private final Offer offer;
  private final TaskInfo task;

  @JsonCreator
  public SingularityTask(@JsonProperty("taskRequest") SingularityTaskRequest taskRequest, @JsonProperty("offer") Offer offer, @JsonProperty("task") TaskInfo task) {
    this.taskRequest = taskRequest;
    this.offer = offer;
    this.task = task;
  }

  public SingularityTaskRequest getTaskRequest() {
    return taskRequest;
  }

  public Offer getOffer() {
    return offer;
  }

  public TaskInfo getTask() {
    return task;
  }

  public byte[] getTaskData(ObjectMapper objectMapper) throws JsonProcessingException {
    return objectMapper.writeValueAsBytes(this);
  }

  public static SingularityTask getTaskFromData(byte[] data, ObjectMapper objectMapper) throws JsonParseException, JsonMappingException, IOException {
    return objectMapper.readValue(data, SingularityTask.class);
  }

  @Override
  public String toString() {
    return "SingularityTask [taskRequest=" + taskRequest + ", offer=" + offer + ", task=" + task + "]";
  }
  
}
