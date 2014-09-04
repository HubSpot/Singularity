package com.hubspot.singularity.logwatcher;

import com.hubspot.singularity.runner.base.shared.TailMetadata;

public interface TailMetadataListener {

  void tailChanged(TailMetadata tailMetadata);

}
