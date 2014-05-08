package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;
import com.hubspot.singularity.SingularityRequest;

public class SingularityRequestTranscoder implements Transcoder<SingularityRequest> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityRequestTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityRequest transcode(byte[] data) {
    return SingularityRequest.fromBytes(data, objectMapper);
  }

  @Override
  public byte[] toBytes(SingularityRequest object) throws SingularityJsonException {
    return object.getAsBytes(objectMapper);
  }
  
  
  
}
