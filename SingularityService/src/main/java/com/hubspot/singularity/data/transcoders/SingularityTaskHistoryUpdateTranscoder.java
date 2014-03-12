package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;

public class SingularityTaskHistoryUpdateTranscoder implements Transcoder<SingularityTaskHistoryUpdate> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityTaskHistoryUpdateTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityTaskHistoryUpdate transcode(byte[] data) throws Exception {
    return SingularityTaskHistoryUpdate.fromBytes(data, objectMapper);
  }
  
}
