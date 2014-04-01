package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;

public class SingularityTaskTranscoder implements Transcoder<SingularityTask>, Function<SingularityTask, SingularityTaskId> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityTaskTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityTask transcode(byte[] data) throws Exception {
    return SingularityTask.fromBytes(data, objectMapper);
  }

  @Override
  public SingularityTaskId apply(SingularityTask input) {
    return input.getTaskId();
  }
  
}
