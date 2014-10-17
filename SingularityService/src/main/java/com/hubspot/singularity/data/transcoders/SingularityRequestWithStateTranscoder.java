package com.hubspot.singularity.data.transcoders;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;
import com.hubspot.singularity.SingularityRequestWithState;

@Singleton
public class SingularityRequestWithStateTranscoder implements Transcoder<SingularityRequestWithState> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityRequestWithStateTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityRequestWithState transcode(byte[] data) {
    return SingularityRequestWithState.fromBytes(data, objectMapper);
  }

  @Override
  public byte[] toBytes(SingularityRequestWithState object) throws SingularityJsonException {
    return object.getAsBytes(objectMapper);
  }

}
