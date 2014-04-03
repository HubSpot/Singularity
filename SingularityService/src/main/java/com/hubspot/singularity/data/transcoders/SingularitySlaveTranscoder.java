package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularitySlave;

public class SingularitySlaveTranscoder implements Transcoder<SingularitySlave> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularitySlaveTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularitySlave transcode(byte[] data) {
    return SingularitySlave.fromBytes(data, objectMapper);
  }

  @Override
  public byte[] toBytes(SingularitySlave object) {
    return object.getAsBytes(objectMapper);
  }
  
}
