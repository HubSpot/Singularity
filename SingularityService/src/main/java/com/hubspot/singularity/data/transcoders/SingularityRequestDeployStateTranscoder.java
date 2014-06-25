package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequestDeployState;

public class SingularityRequestDeployStateTranscoder implements Transcoder<SingularityRequestDeployState> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityRequestDeployStateTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityRequestDeployState transcode(byte[] data) {
    return SingularityRequestDeployState.fromBytes(data, objectMapper);
  }
  
  @Override
  public byte[] toBytes(SingularityRequestDeployState object) {
    return object.getAsBytes(objectMapper);
  }
  
}
