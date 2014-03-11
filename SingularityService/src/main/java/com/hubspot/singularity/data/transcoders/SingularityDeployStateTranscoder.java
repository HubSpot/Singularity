package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployState;

public class SingularityDeployStateTranscoder implements Transcoder<SingularityDeployState> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityDeployStateTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityDeployState transcode(byte[] data) throws Exception {
    return SingularityDeployState.fromBytes(data, objectMapper);
  }
  
}
