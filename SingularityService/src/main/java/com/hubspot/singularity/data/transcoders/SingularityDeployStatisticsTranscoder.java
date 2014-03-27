package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployStatistics;

public class SingularityDeployStatisticsTranscoder implements Transcoder<SingularityDeployStatistics> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityDeployStatisticsTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityDeployStatistics transcode(byte[] data) throws Exception {
    return SingularityDeployStatistics.fromBytes(data, objectMapper);
  }
  
}
