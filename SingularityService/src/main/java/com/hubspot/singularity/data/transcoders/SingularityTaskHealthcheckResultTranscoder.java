package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;

public class SingularityTaskHealthcheckResultTranscoder extends SingularityTaskIdHolderTranscoder<SingularityTaskHealthcheckResult> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityTaskHealthcheckResultTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public byte[] toBytes(SingularityTaskHealthcheckResult object) {
    return object.getAsBytes(objectMapper);
  }

  @Override
  public SingularityTaskHealthcheckResult transcode(byte[] data) {
    return SingularityTaskHealthcheckResult.fromBytes(data, objectMapper);
  }
  
}
