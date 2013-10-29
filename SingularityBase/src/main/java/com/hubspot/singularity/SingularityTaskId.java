package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;

public class SingularityTaskId implements Comparable<SingularityTaskId> {

  private final String name;
  private final long nextRunAt;
  private final int instanceNo;

  @JsonCreator
  public SingularityTaskId(@JsonProperty("name") String name, @JsonProperty("nextRunAt") long nextRunAt, @JsonProperty("instanceNo") int instanceNo) {
    this.name = name;
    this.nextRunAt = nextRunAt;
    this.instanceNo = instanceNo;
  }

  public String getName() {
    return name;
  }

  public long getNextRunAt() {
    return nextRunAt;
  }

  public int getInstanceNo() {
    return instanceNo;
  }
  
  public static SingularityTaskId fromString(String string) {
    final String[] splits = string.split("\\-");
 
    final int instanceNo = Integer.parseInt(splits[splits.length - 1]);
    final long nextRunAt = Long.parseLong(splits[splits.length - 2]);
    
    StringBuilder nameBldr = new StringBuilder();
    
    for (int s = 0; s < splits.length - 2; s++) {
      nameBldr.append(splits[s]);
      if (s < splits.length - 3) {
        nameBldr.append("-");
      }
    }
     
    final String name = nameBldr.toString();
    
    return new SingularityTaskId(name, nextRunAt, instanceNo);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + instanceNo;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + (int) (nextRunAt ^ (nextRunAt >>> 32));
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
    if (nextRunAt != other.nextRunAt)
      return false;
    return true;
  }

  public String toString() {
    return String.format("%s-%s-%s", getName(), getNextRunAt(), getInstanceNo());
  }
  
  @Override
  public int compareTo(SingularityTaskId o) {
    return ComparisonChain.start()
        .compare(this.getNextRunAt(), o.getNextRunAt())
        .compare(this.getName(), o.getName())
        .compare(this.getInstanceNo(), o.getInstanceNo())
        .result();
  }
  

}
