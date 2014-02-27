package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRack;

public class SingularityRackTranscoder implements Transcoder<SingularityRack> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityRackTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityRack transcode(byte[] data) throws Exception {
    return SingularityRack.fromBytes(data, objectMapper);
  }
  
}
