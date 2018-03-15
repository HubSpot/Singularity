package com.hubspot.singularity.api.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface SingularityHistoryItem {

  @JsonIgnore
  long getCreateTimestampForCalculatingHistoryAge();

}
