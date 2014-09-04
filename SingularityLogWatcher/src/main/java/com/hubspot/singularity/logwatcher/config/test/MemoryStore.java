package com.hubspot.singularity.logwatcher.config.test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.hubspot.singularity.logwatcher.SimpleStore;
import com.hubspot.singularity.logwatcher.TailMetadataListener;
import com.hubspot.singularity.runner.base.shared.TailMetadata;

public class MemoryStore implements SimpleStore {

  private static final Logger LOG = LoggerFactory.getLogger(MemoryStore.class);

  private final Map<TailMetadata, Long> map;
  private final List<TailMetadata> list;

  public MemoryStore(List<TailMetadata> list) {
    this.map = Maps.newHashMap();
    this.list = list;
  }

  @Override
  public void close() throws IOException {}

  @Override
  public void start() {}

  @Override
  public void markConsumed(TailMetadata tail) throws StoreException {
    LOG.info("Consumed:" + tail);
  }

  @Override
  public void savePosition(TailMetadata tail, long position) {
    Long previous = map.put(tail, position);
    LOG.info("Stored position {} for {}, old position {}", position, tail, previous);
  }

  @Override
  public Optional<Long> getPosition(TailMetadata tail) {
    return Optional.fromNullable(map.get(tail));
  }

  @Override
  public List<TailMetadata> getTails() {
    return list;
  }

  @Override
  public void registerListener(TailMetadataListener listener) {
  }


  @Override
  public void removeListener(TailMetadataListener listener) {
  }

}
