package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityTaskTranscoder extends CompressingTranscoder implements Transcoder<SingularityTask>, Function<SingularityTask, SingularityTaskId> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityTaskTranscoder(SingularityConfiguration configuration, ObjectMapper objectMapper) {
    super(configuration);
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityTask transcode(byte[] data) {
    return SingularityTask.fromBytes(getMaybeUncompressedBytes(data), objectMapper);
  }
  
  @Override
  public byte[] toBytes(SingularityTask object) throws SingularityJsonException {
    return getMaybeCompressedBytes(object.getAsBytes(objectMapper));
  }

  @Override
  public SingularityTaskId apply(SingularityTask input) {
    return input.getTaskId();
  }
  
}
