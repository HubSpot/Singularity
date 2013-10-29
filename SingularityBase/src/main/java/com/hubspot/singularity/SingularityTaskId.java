package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskId {

  private final String name;
  private final long startedAt;
  private final int instanceNo;
  private final String rackId;

  @JsonCreator
  public SingularityTaskId(@JsonProperty("name") String name, @JsonProperty("nextRunAt") long startedAt, @JsonProperty("instanceNo") int instanceNo, @JsonProperty("rackId") String rackId) {
    this.name = name;
    this.startedAt = startedAt;
    this.instanceNo = instanceNo;
    this.rackId = rackId;
  }
  
  public String getRackId() {
    return rackId;
  }
  
  @JsonIgnore
  public String getSafeRackId() {
    return rackId.replace("-", "");
  }

  public String getName() {
    return name;
  }

  public long getStartedAt() {
    return startedAt;
  }

  public int getInstanceNo() {
    return instanceNo;
  }
  
  public boolean matches(SingularityPendingTaskId pendingTaskId) {
    return getName().equals(pendingTaskId.getName());
  }
  
  public static SingularityTaskId fromString(String string) {
    final String[] splits = string.split("\\-");
    
    final String rackId = splits[splits.length - 1];
    final int instanceNo = Integer.parseInt(splits[splits.length - 2]);
    final long startedAt = Long.parseLong(splits[splits.length - 3]);
    
    StringBuilder nameBldr = new StringBuilder();
    
    for (int s = 0; s < splits.length - 2; s++) {
      nameBldr.append(splits[s]);
      if (s < splits.length - 3) {
        nameBldr.append("-");
      }
    }
     
    final String name = nameBldr.toString();
    
    return new SingularityTaskId(name, startedAt, instanceNo, rackId);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + instanceNo;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((rackId == null) ? 0 : rackId.hashCode());
    result = prime * result + (int) (startedAt ^ (startedAt >>> 32));
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
    SingularityTaskId other = (SingularityTaskId) obj;
    if (instanceNo != other.instanceNo)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (rackId == null) {
      if (other.rackId != null)
        return false;
    } else if (!rackId.equals(other.rackId))
      return false;
    if (startedAt != other.startedAt)
      return false;
    return true;
  }

  public String toString() {
    return String.format("%s-%s-%s-%s", getName(), getStartedAt(), getInstanceNo(), getSafeRackId());
  }
  
}
