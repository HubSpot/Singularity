package com.hubspot.singularity.data.transcoders;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.hubspot.singularity.SingularityDeployKey;

@Singleton
public class SingularityDeployKeyTranscoder extends IdTranscoder<SingularityDeployKey> {

  @Inject
  public SingularityDeployKeyTranscoder()
  {}

  @Override
  public SingularityDeployKey transcode(final String id) {
    return SingularityDeployKey.fromString(id);
  }

}
