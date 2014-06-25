package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;
import com.hubspot.singularity.SingularityPendingRequest;

public class SingularityPendingRequestTranscoder implements Transcoder<SingularityPendingRequest> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityPendingRequestTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityPendingRequest transcode(byte[] data) {
    return SingularityPendingRequest.fromBytes(data, objectMapper);
  }

  @Override
  public byte[] toBytes(SingularityPendingRequest object) throws SingularityJsonException {
    return object.getAsBytes(objectMapper);
  }
  
}
