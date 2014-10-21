package com.hubspot.singularity.data.transcoders;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;
import com.hubspot.singularity.SingularityRack;

@Singleton
public class SingularityRackTranscoder implements Transcoder<SingularityRack> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityRackTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityRack transcode(byte[] data) {
    return SingularityRack.fromBytes(data, objectMapper);
  }

  @Override
  public byte[] toBytes(SingularityRack object) throws SingularityJsonException {
    return object.getAsBytes(objectMapper);
  }

}
