package com.hubspot.singularity.data.transcoders;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;
import com.hubspot.singularity.SingularityRequestCleanup;

@Singleton
public class SingularityRequestCleanupTranscoder implements Transcoder<SingularityRequestCleanup> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityRequestCleanupTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityRequestCleanup transcode(byte[] data) {
    return SingularityRequestCleanup.fromBytes(data, objectMapper);
  }

  @Override
  public byte[] toBytes(SingularityRequestCleanup object) throws SingularityJsonException {
    return object.getAsBytes(objectMapper);
  }



}
