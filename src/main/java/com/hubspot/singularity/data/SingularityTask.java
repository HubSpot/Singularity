package com.hubspot.singularity.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.SingularityRequest;

public class SingularityTask {

  private final SingularityRequest request;
  private final String guid;
  
  private String taskId;
  
  @JsonCreator
  public SingularityTask(@JsonProperty("request") SingularityRequest request, @JsonProperty("guid") String guid) {
    this.request = request;
    this.guid = guid;
  }
  
  public SingularityRequest getRequest() {
    return request;
  }
  
  public String getGuid() {
    return guid;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((guid == null) ? 0 : guid.hashCode());
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
    if (guid == null) {
      if (other.guid != null)
        return false;
    } else if (!guid.equals(other.guid))
      return false;
    return true;
  }
  
  
  
}
