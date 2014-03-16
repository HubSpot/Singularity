package com.hubspot.singularity;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityTaskRequest extends SingularityJsonObject implements Comparable<SingularityTaskRequest> {

  private final SingularityRequest request;
  private final SingularityPendingTaskId pendingTaskId;
  private final Optional<String> maybeCmdLineArgs;
    
  @JsonCreator
  public SingularityTaskRequest(@JsonProperty("request") SingularityRequest request, @JsonProperty("pendingTaskId") SingularityPendingTaskId pendingTaskId, @JsonProperty("cmdLineArgs") Optional<String> cmdLineArgs) {
    this.request = request;
    this.pendingTaskId = pendingTaskId;
    this.maybeCmdLineArgs = cmdLineArgs;
  }
  
  public SingularityRequest getRequest() {
    return request;
  }
  
  // TODO next data migration, this should move to being SingularityPendingTask
  public SingularityPendingTaskId getPendingTaskId() {
    return pendingTaskId;
  }
  
  public Optional<String> getMaybeCmdLineArgs() {
    return maybeCmdLineArgs;
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
    return "SingularityTaskRequest [request=" + request + ", pendingTaskId=" + pendingTaskId + ", maybeCmdLineArgs=" + maybeCmdLineArgs + "]";
  }

}
