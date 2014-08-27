package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityDeployHistoryTranscoder extends CompressingTranscoder<SingularityDeployHistory> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityDeployHistoryTranscoder(SingularityConfiguration configuration, ObjectMapper objectMapper) {
    super(configuration);
    this.objectMapper = objectMapper;
  }

  @Override
  protected byte[] actualToBytes(SingularityDeployHistory object) {
    return object.getAsBytes(objectMapper);
  }

  @Override
  protected SingularityDeployHistory actualTranscode(byte[] data) {
    return SingularityDeployHistory.fromBytes(data, objectMapper);
  }

}
