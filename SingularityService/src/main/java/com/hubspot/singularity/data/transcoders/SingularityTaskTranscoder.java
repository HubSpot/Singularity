package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityTaskTranscoder extends SingularityCompressingTaskIdHolderTranscoder<SingularityTask> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityTaskTranscoder(SingularityConfiguration configuration, ObjectMapper objectMapper) {
    super(configuration);
    this.objectMapper = objectMapper;
  }

  @Override
  protected SingularityTask actualTranscode(byte[] data) {
    return SingularityTask.fromBytes(data, objectMapper);
  }

  @Override
  protected byte[] actualToBytes(SingularityTask object) {
    return object.getAsBytes(objectMapper);
  }

}
