package com.hubspot.singularity.logwatcher;

import com.hubspot.singularity.runner.base.config.TailMetadata;

public interface TailMetadataListener {

  public void tailChanged(TailMetadata tailMetadata);
  
}
