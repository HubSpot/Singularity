package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeploy;

public class SingularityDeployTranscoder implements Transcoder<SingularityDeploy> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityDeployTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityDeploy transcode(byte[] data) throws Exception {
    return SingularityDeploy.fromBytes(data, objectMapper);
  }
  
}
