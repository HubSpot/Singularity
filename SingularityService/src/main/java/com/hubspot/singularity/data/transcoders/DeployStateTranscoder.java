package com.hubspot.singularity.data.transcoders;

import com.hubspot.singularity.DeployState;

public class DeployStateTranscoder extends EnumTranscoder<DeployState> {

  public final static DeployStateTranscoder DEPLOY_STATE_TRANSCODER = new DeployStateTranscoder();

  @Override
  protected DeployState fromString(String string) {
    return DeployState.valueOf(string);
  }
  
}
