package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployMarker;

public class SingularityDeployMarkerTranscoder implements Transcoder<SingularityDeployMarker> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityDeployMarkerTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityDeployMarker transcode(byte[] data) {
    return SingularityDeployMarker.fromBytes(data, objectMapper);
  }

  @Override
  public byte[] toBytes(SingularityDeployMarker object) {
    return object.getAsBytes(objectMapper);
  }

}
