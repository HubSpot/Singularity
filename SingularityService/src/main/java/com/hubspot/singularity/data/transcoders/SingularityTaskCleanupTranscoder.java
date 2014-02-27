package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskCleanup;

public class SingularityTaskCleanupTranscoder implements Transcoder<SingularityTaskCleanup> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityTaskCleanupTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityTaskCleanup transcode(byte[] data) throws Exception {
    return SingularityTaskCleanup.fromBytes(data, objectMapper);
  }
  
}
