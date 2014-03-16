package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTask;

public class SingularityTaskTranscoder implements Transcoder<SingularityTask> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityTaskTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityTask transcode(byte[] data) throws Exception {
    return SingularityTask.fromBytes(data, objectMapper);
  }
  
}
