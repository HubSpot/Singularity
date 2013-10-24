package com.hubspot.singularity.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.singularity.SingularityRequest;

public class SingularityTask implements Comparable<SingularityTask> {

  private final SingularityRequest request;
  private final SingularityTaskId taskId;
  
  @JsonCreator
  public SingularityTask(@JsonProperty("request") SingularityRequest request, @JsonProperty("taskId") SingularityTaskId taskId) {
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
    SingularityTask other = (SingularityTask) obj;
    if (taskId == null) {
      if (other.taskId != null)
        return false;
    } else if (!taskId.equals(other.taskId))
      return false;
    return true;
  }

  @Override
  public int compareTo(SingularityTask o) {
    return this.getTaskId().compareTo(o.getTaskId());
  }
  
  public byte[] getTaskData(ObjectMapper objectMapper) throws Exception {
    return objectMapper.writeValueAsBytes(this);
  }

  public static SingularityTask getTaskFromData(byte[] data, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(data, SingularityTask.class);
  }
  
}
