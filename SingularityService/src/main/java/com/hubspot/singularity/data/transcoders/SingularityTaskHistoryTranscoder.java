package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityTaskHistoryTranscoder extends CompressingTranscoder implements Transcoder<SingularityTaskHistory> {

  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityTaskHistoryTranscoder(SingularityConfiguration configuration, ObjectMapper objectMapper) {
    super(configuration);
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityTaskHistory transcode(byte[] data) {
    return SingularityTaskHistory.fromBytes(getMaybeUncompressedBytes(data), objectMapper);
  }
  
  @Override
  public byte[] toBytes(SingularityTaskHistory object) throws SingularityJsonException {
    return getMaybeCompressedBytes(object.getAsBytes(objectMapper));
  }
  
}
