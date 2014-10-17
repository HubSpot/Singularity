package com.hubspot.singularity.data.transcoders;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityStateTranscoder extends CompressingTranscoder<SingularityState> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityStateTranscoder(ObjectMapper objectMapper, SingularityConfiguration configuration) {
    super(configuration);
    this.objectMapper = objectMapper;
  }

  @Override
  protected SingularityState actualTranscode(byte[] data) {
    return SingularityState.fromBytes(data, objectMapper);
  }


  @Override
  protected byte[] actualToBytes(SingularityState object) {
    return object.getAsBytes(objectMapper);
  }

}
