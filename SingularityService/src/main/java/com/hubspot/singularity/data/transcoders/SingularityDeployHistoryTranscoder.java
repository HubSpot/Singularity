package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityDeployHistoryTranscoder extends CompressingTranscoder implements Transcoder<SingularityDeployHistory> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityDeployHistoryTranscoder(SingularityConfiguration configuration, ObjectMapper objectMapper) {
    super(configuration);
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityDeployHistory transcode(byte[] data) {
    return SingularityDeployHistory.fromBytes(getMaybeUncompressedBytes(data), objectMapper);
  }
  
  @Override
  public byte[] toBytes(SingularityDeployHistory object) throws SingularityJsonException {
    return getMaybeCompressedBytes(object.getAsBytes(objectMapper));
  }
  
}
