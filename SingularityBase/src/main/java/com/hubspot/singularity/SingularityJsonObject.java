package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityJsonObject {

  @JsonIgnore
  public byte[] getAsBytes(ObjectMapper objectMapper) throws JsonProcessingException {
    return objectMapper.writeValueAsBytes(this);
  }
  
}
