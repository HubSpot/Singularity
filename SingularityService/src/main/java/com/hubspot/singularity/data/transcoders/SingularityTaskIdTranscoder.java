package com.hubspot.singularity.data.transcoders;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.hubspot.singularity.SingularityTaskId;

@Singleton
public class SingularityTaskIdTranscoder extends IdTranscoder<SingularityTaskId> {

  @Inject
  public SingularityTaskIdTranscoder()
  {}

  @Override
  public SingularityTaskId transcode(final String id) {
    return SingularityTaskId.fromString(id);
  }

}
