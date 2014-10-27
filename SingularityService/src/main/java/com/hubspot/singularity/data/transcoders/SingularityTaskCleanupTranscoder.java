package com.hubspot.singularity.data.transcoders;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskCleanup;

@Singleton
public class SingularityTaskCleanupTranscoder implements Transcoder<SingularityTaskCleanup> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityTaskCleanupTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public byte[] toBytes(SingularityTaskCleanup object)  {
    return object.getAsBytes(objectMapper);
  }

  @Override
  public SingularityTaskCleanup transcode(byte[] data) {
    return SingularityTaskCleanup.fromBytes(data, objectMapper);
  }

}
