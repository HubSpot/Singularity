package com.hubspot.singularity.data.transcoders;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;

@Singleton
public class SingularityLoadBalancerUpdateTranscoder implements Transcoder<SingularityLoadBalancerUpdate> {

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityLoadBalancerUpdateTranscoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public byte[] toBytes(SingularityLoadBalancerUpdate object)  {
    return object.getAsBytes(objectMapper);
  }

  @Override
  public SingularityLoadBalancerUpdate transcode(byte[] data) {
    return SingularityLoadBalancerUpdate.fromBytes(data, objectMapper);
  }

}
