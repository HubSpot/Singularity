package com.hubspot.singularity.data.transcoders;

import com.hubspot.singularity.SingularityDeployKey;

public class SingularityDeployKeyTranscoder extends IdTranscoder<SingularityDeployKey> {

  @Override
  public SingularityDeployKey transcode(String id) {
    return SingularityDeployKey.fromString(id);
  }

}
