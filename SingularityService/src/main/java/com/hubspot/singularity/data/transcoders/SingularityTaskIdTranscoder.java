package com.hubspot.singularity.data.transcoders;

import com.hubspot.singularity.SingularityTaskId;

public class SingularityTaskIdTranscoder extends IdTranscoder<SingularityTaskId> {

  @Override
  public SingularityTaskId transcode(String id) {
    return SingularityTaskId.fromString(id);
  }

}
