package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityDeployTranscoder extends CompressingTranscoder<SingularityDeploy> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityDeployTranscoder(ObjectMapper objectMapper, SingularityConfiguration configuration) {
    super(configuration);
    this.objectMapper = objectMapper;
  }

  @Override
  protected SingularityDeploy actualTranscode(byte[] data) {
    return SingularityDeploy.fromBytes(data, objectMapper);
  }


  @Override
  protected byte[] actualToBytes(SingularityDeploy object) {
    return object.getAsBytes(objectMapper);
  }

}
