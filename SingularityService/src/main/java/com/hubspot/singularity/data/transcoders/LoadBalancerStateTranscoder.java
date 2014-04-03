package com.hubspot.singularity.data.transcoders;

import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.LoadBalancerState;

public class LoadBalancerStateTranscoder implements Transcoder<LoadBalancerState> {

  public final static LoadBalancerStateTranscoder LOAD_BALANCER_STATE_TRANSCODER = new LoadBalancerStateTranscoder();
  
  @Override
  public LoadBalancerState transcode(byte[] data) {
    return LoadBalancerState.valueOf(JavaUtils.toString(data));
  }
  
  public byte[] toBytes(LoadBalancerState loadBalancerState) {
    return JavaUtils.toBytes(loadBalancerState.name());
  }

}
