package com.hubspot.singularity.data.transcoders;

import javax.inject.Inject;

import com.hubspot.singularity.SingularityPendingTaskId;

public class SingularityPendingTaskIdTranscoder extends IdTranscoder<SingularityPendingTaskId> {

  @Inject
  public SingularityPendingTaskIdTranscoder()
  {}

  @Override
  public SingularityPendingTaskId transcode(final String id) {
    return SingularityPendingTaskId.fromString(id);
  }

}
