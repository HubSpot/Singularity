package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityDeployTranscoder extends CompressingTranscoder implements Transcoder<SingularityDeploy> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityDeployTranscoder(ObjectMapper objectMapper, SingularityConfiguration configuration) {
    super(configuration);
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityDeploy transcode(byte[] data) {
    return SingularityDeploy.fromBytes(getMaybeUncompressedBytes(data), objectMapper);
  }
  
  @Override
  public byte[] toBytes(SingularityDeploy object) {
    return getMaybeCompressedBytes(object.getAsBytes(objectMapper));
  }
  
}
