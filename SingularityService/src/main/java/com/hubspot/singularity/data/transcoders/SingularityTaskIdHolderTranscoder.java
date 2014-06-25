package com.hubspot.singularity.data.transcoders;

import com.google.common.base.Function;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHolder;

public abstract class SingularityTaskIdHolderTranscoder<K extends SingularityTaskIdHolder> implements Transcoder<K>, Function<K, SingularityTaskId> {
  
  @Override
  public SingularityTaskId apply(K input) {
    return input.getTaskId();
  }

}
