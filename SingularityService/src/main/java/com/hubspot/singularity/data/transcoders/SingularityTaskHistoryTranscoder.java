package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityTaskHistoryTranscoder extends CompressingTranscoder<SingularityTaskHistory> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityTaskHistoryTranscoder(SingularityConfiguration configuration, ObjectMapper objectMapper) {
    super(configuration);
    this.objectMapper = objectMapper;
  }

  @Override
  protected SingularityTaskHistory actualTranscode(byte[] data) {
    return SingularityTaskHistory.fromBytes(data, objectMapper);
  }

  @Override
  protected byte[] actualToBytes(SingularityTaskHistory object) {
    return object.getAsBytes(objectMapper);
  }

}
