package com.hubspot.singularity.data.transcoders;

import javax.inject.Inject;

import com.hubspot.singularity.SingularityDeployKey;

public class SingularityDeployKeyTranscoder extends IdTranscoder<SingularityDeployKey> {

  @Inject
  public SingularityDeployKeyTranscoder()
  {}

  @Override
  public SingularityDeployKey transcode(final String id) {
    return SingularityDeployKey.fromString(id);
  }

}
