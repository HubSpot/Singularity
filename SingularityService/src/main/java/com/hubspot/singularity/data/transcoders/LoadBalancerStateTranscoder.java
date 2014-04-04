package com.hubspot.singularity.data.transcoders;

import com.hubspot.singularity.LoadBalancerState;

public class LoadBalancerStateTranscoder extends EnumTranscoder<LoadBalancerState> {

  public final static LoadBalancerStateTranscoder LOAD_BALANCER_STATE_TRANSCODER = new LoadBalancerStateTranscoder();

  @Override
  protected LoadBalancerState fromString(String string) {
    return LoadBalancerState.valueOf(string);
  }
  
}
