package com.hubspot.singularity.data.transcoders;

import com.hubspot.singularity.SingularityPendingTaskId;

public class SingularityPendingTaskIdTranscoder extends IdTranscoder<SingularityPendingTaskId> {

  @Override
  public SingularityPendingTaskId transcode(String id) {
    return SingularityPendingTaskId.fromString(id);
  }

}
