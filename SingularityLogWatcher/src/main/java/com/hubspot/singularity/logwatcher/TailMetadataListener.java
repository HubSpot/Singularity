package com.hubspot.singularity.logwatcher;

import com.hubspot.singularity.runner.base.shared.TailMetadata;

public interface TailMetadataListener {

  public void tailChanged(TailMetadata tailMetadata);
  
}
