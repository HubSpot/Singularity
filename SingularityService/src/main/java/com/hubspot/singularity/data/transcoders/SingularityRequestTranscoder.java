package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequest;

public class SingularityRequestTranscoder implements Transcoder<SingularityRequest> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityRequestTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityRequest transcode(byte[] data) throws Exception {
    return SingularityRequest.fromBytes(data, objectMapper);
  }
  
}
