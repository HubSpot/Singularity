package com.hubspot.singularity.data.transcoders;

import com.google.common.base.Function;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHolder;
import com.hubspot.singularity.config.SingularityConfiguration;

public abstract class SingularityCompressingTaskIdHolderTranscoder<K extends SingularityTaskIdHolder> extends CompressingTranscoder<K> implements Function<K, SingularityTaskId> {
  
  public SingularityCompressingTaskIdHolderTranscoder(SingularityConfiguration configuration) {
    super(configuration);
  }

  @Override
  public SingularityTaskId apply(K input) {
    return input.getTaskId();
  }

}
