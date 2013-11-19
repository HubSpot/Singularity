package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class SingularityId {

  public String getId() {
    return toString();
  }

}
