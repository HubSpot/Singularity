package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;
import com.hubspot.singularity.SingularityPendingDeploy;

public class SingularityPendingDeployTranscoder implements Transcoder<SingularityPendingDeploy> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityPendingDeployTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityPendingDeploy transcode(byte[] data) {
    return SingularityPendingDeploy.fromBytes(data, objectMapper);
  }
  
  @Override
  public byte[] toBytes(SingularityPendingDeploy object) throws SingularityJsonException {
    return object.getAsBytes(objectMapper);
  }
}
