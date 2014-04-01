package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityPendingDeploy;

public class SingularityPendingDeployTranscoder implements Transcoder<SingularityPendingDeploy> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityPendingDeployTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityPendingDeploy transcode(byte[] data) throws Exception {
    return SingularityPendingDeploy.fromBytes(data, objectMapper);
  }
  
}
