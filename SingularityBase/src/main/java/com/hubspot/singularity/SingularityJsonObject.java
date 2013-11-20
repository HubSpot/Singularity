package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityJsonObject {

  @JsonIgnore
  public byte[] getAsBytes(ObjectMapper objectMapper) throws SingularityJsonException {
    try {
      return objectMapper.writeValueAsBytes(this);
    } catch (JsonProcessingException jpe) {
      throw new SingularityJsonException(jpe);
    }
  }
  
  public static class SingularityJsonException extends RuntimeException {

    public SingularityJsonException(Throwable cause) {
      super(cause);
    }
    
  }
  
}
