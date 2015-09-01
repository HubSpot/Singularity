package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface SingularityHistoryItem {

  @JsonIgnore
  long getCreateTimestampForCalculatingHistoryAge();

}
