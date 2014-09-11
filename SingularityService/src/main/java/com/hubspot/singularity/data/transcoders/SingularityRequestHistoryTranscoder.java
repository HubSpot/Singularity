package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityRequestHistoryTranscoder extends CompressingTranscoder<SingularityRequestHistory> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityRequestHistoryTranscoder(ObjectMapper objectMapper, SingularityConfiguration configuration) {
    super(configuration);

    this.objectMapper = objectMapper;
  }

  @Override
  protected SingularityRequestHistory actualTranscode(byte[] data) {
    return SingularityRequestHistory.fromBytes(data, objectMapper);
  }

  @Override
  protected byte[] actualToBytes(SingularityRequestHistory object) {
    return object.getAsBytes(objectMapper);
  }

}
