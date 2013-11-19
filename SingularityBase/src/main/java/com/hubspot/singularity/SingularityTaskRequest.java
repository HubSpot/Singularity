package com.hubspot.singularity;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityTaskRequest extends SingularityJsonObject implements Comparable<SingularityTaskRequest> {

  private final SingularityRequest request;
  private final SingularityPendingTaskId pendingTaskId;
  
  @JsonCreator
  public SingularityTaskRequest(@JsonProperty("request") SingularityRequest request, @JsonProperty("pendingTaskId") SingularityPendingTaskId pendingTaskId) {
    this.request = request;
    this.pendingTaskId = pendingTaskId;
  }
  
  public SingularityRequest getRequest() {
    return request;
  }
  
  public SingularityPendingTaskId getPendingTaskId() {
    return pendingTaskId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pendingTaskId);
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
    if (pendingTaskId == null) {
      if (other.pendingTaskId != null)
        return false;
    } else if (!pendingTaskId.equals(other.pendingTaskId))
      return false;
    return true;
  }

  @Override
  public int compareTo(SingularityTaskRequest o) {
    return this.getPendingTaskId().compareTo(o.getPendingTaskId());
  }
  
  public static SingularityTaskRequest fromBytes(byte[] bytes, ObjectMapper objectMapper) throws JsonParseException, JsonMappingException, IOException {
    return objectMapper.readValue(bytes, SingularityTaskRequest.class);
  }

  @Override
  public String toString() {
    return "SingularityTaskRequest [request=" + request + ", pendingtaskId=" + pendingTaskId + "]";
  }
  
}
