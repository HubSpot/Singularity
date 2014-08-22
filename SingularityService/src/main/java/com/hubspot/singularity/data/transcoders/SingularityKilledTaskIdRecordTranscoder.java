package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;

public class SingularityKilledTaskIdRecordTranscoder implements Transcoder<SingularityKilledTaskIdRecord> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityKilledTaskIdRecordTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public byte[] toBytes(SingularityKilledTaskIdRecord object)  {
    return object.getAsBytes(objectMapper);
  }

  @Override
  public SingularityKilledTaskIdRecord transcode(byte[] data) {
    return SingularityKilledTaskIdRecord.fromBytes(data, objectMapper);
  }

}
