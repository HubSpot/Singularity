package com.hubspot.singularity.logwatcher.config.test;

import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.hubspot.singularity.logwatcher.SimpleStore;

public class MemoryStore implements SimpleStore {

  private final static Logger LOG = LoggerFactory.getLogger(MemoryStore.class);

  private final Map<Path, Long> map;
  
  public MemoryStore() {
    map = Maps.newHashMap();
  }
  
  @Override
  public void savePosition(Path logfile, long position) {
    Long previous = map.put(logfile, position);
    LOG.info("Stored position {} for {}, old position {}", position, logfile, previous);
  }

  @Override
  public Optional<Long> getPosition(Path logfile) {
    return Optional.fromNullable(map.get(logfile));
  }

}
