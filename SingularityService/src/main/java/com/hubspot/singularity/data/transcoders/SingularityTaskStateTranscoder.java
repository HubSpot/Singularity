package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskState;

public class SingularityTaskStateTranscoder extends SingularityTaskIdHolderTranscoder<SingularityTaskState> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityTaskStateTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityTaskState transcode(byte[] data) throws Exception {
    return SingularityTaskState.fromBytes(data, objectMapper);
  }
  
}
