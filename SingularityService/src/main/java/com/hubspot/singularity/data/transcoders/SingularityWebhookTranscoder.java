package com.hubspot.singularity.data.transcoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityWebhook;

public class SingularityWebhookTranscoder implements Transcoder<SingularityWebhook> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityWebhookTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public SingularityWebhook transcode(byte[] data) {
    return SingularityWebhook.fromBytes(data, objectMapper);
  }

  @Override
  public byte[] toBytes(SingularityWebhook object) {
    return object.getAsBytes(objectMapper);
  }

}
