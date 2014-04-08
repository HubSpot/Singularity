package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;

public class SingularityDeployStateTranscoder implements Transcoder<SingularityDeployResult> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityDeployStateTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityDeployResult transcode(byte[] data) {
    return SingularityDeployResult.fromBytes(data, objectMapper);
  }
  
  @Override
  public byte[] toBytes(SingularityDeployResult object) throws SingularityJsonException {
    return object.getAsBytes(objectMapper);
  }
  
}
