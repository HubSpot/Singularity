package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployState;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;

public class SingularityDeployStateTranscoder implements Transcoder<SingularityDeployState> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityDeployStateTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityDeployState transcode(byte[] data) {
    return SingularityDeployState.fromBytes(data, objectMapper);
  }
  
  @Override
  public byte[] toBytes(SingularityDeployState object) throws SingularityJsonException {
    return object.getAsBytes(objectMapper);
  }
  
}
