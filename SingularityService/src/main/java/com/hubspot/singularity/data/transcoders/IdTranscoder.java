package com.hubspot.singularity.data.transcoders;

import com.google.common.base.Function;
import com.hubspot.singularity.SingularityId;

public abstract class IdTranscoder<T extends SingularityId> implements Function<String, T> {

  public abstract T transcode(String id);

  @Override
  public T apply(String input) {
    return transcode(input);
  }

}
