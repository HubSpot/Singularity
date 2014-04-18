package com.hubspot.singularity.logwatcher;

import java.nio.file.Path;

import com.google.common.base.Optional;

public interface SimpleStore {

  public void savePosition(Path logfile, long position);
 
  public Optional<Long> getPosition(Path logfile);

}
