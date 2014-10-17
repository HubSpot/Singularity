package com.hubspot.singularity.data.transcoders;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployWebhook;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityDeployWebhookTranscoder extends CompressingTranscoder<SingularityDeployWebhook> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityDeployWebhookTranscoder(SingularityConfiguration configuration, ObjectMapper objectMapper) {
    super(configuration);
    this.objectMapper = objectMapper;
  }

  @Override
  protected byte[] actualToBytes(SingularityDeployWebhook object) {
    return object.getAsBytes(objectMapper);
  }

  @Override
  protected SingularityDeployWebhook actualTranscode(byte[] data) {
    return SingularityDeployWebhook.fromBytes(data, objectMapper);
  }

}
