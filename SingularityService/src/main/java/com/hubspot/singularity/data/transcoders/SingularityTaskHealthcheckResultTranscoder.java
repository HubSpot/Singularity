package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityTaskHealthcheckResultTranscoder extends SingularityCompressingTaskIdHolderTranscoder<SingularityTaskHealthcheckResult> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityTaskHealthcheckResultTranscoder(SingularityConfiguration configuration, ObjectMapper objectMapper) {
    super(configuration);
    this.objectMapper = objectMapper;
  }

  @Override
  protected SingularityTaskHealthcheckResult actualTranscode(byte[] data) {
    return SingularityTaskHealthcheckResult.fromBytes(data, objectMapper);
  }

  @Override
  protected byte[] actualToBytes(SingularityTaskHealthcheckResult object) {
    return object.getAsBytes(objectMapper);
  }

}
