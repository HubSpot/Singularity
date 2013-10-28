package com.hubspot.singularity.data;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.singularity.SingularityRequest;

public class SingularityTaskRequest implements Comparable<SingularityTaskRequest> {

  private final SingularityRequest request;
  private final SingularityTaskId taskId;
  
  @JsonCreator
  public SingularityTaskRequest(@JsonProperty("request") SingularityRequest request, @JsonProperty("taskId") SingularityTaskId taskId) {
    this.request = request;
    this.taskId = taskId;
  }
  
  public SingularityRequest getRequest() {
    return request;
  }
  
  public SingularityTaskId getTaskId() {
    return taskId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((taskId == null) ? 0 : taskId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SingularityTaskRequest other = (SingularityTaskRequest) obj;
    if (taskId == null) {
      if (other.taskId != null)
        return false;
    } else if (!taskId.equals(other.taskId))
      return false;
    return true;
  }

  @Override
  public int compareTo(SingularityTaskRequest o) {
    return this.getTaskId().compareTo(o.getTaskId());
  }
  
  public byte[] getTaskData(ObjectMapper objectMapper) throws JsonProcessingException {
    return objectMapper.writeValueAsBytes(this);
  }

  public static SingularityTaskRequest getTaskFromData(byte[] data, ObjectMapper objectMapper) throws JsonParseException, JsonMappingException, IOException {
    return objectMapper.readValue(data, SingularityTaskRequest.class);
  }

  @Override
  public String toString() {
    return "SingularityTaskRequest [request=" + request + ", taskId=" + taskId + "]";
  }
  
}
