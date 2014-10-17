package com.hubspot.singularity.data.transcoders;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityTaskStatusTranscoder extends CompressingTranscoder<SingularityTaskStatusHolder> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityTaskStatusTranscoder(SingularityConfiguration configuration, ObjectMapper objectMapper) {
    super(configuration);

    this.objectMapper = objectMapper;
  }

  @Override
  protected SingularityTaskStatusHolder actualTranscode(byte[] data) {
    return SingularityTaskStatusHolder.fromBytes(data, objectMapper);
  }

  @Override
  protected byte[] actualToBytes(SingularityTaskStatusHolder object) {
    return object.getAsBytes(objectMapper);
  }

}
