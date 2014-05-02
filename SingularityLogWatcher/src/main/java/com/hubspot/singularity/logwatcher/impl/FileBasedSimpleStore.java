package com.hubspot.singularity.logwatcher.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.logwatcher.SimpleStore;
import com.hubspot.singularity.logwatcher.TailMetadataListener;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseConfiguration;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.shared.TailMetadata;
import com.hubspot.singularity.runner.base.shared.WatchServiceHelper;

public class FileBasedSimpleStore extends WatchServiceHelper implements SimpleStore {

  private final static Logger LOG = LoggerFactory.getLogger(FileBasedSimpleStore.class);

  private final SingularityRunnerBaseConfiguration baseConfiguration;
  private final SingularityLogWatcherConfiguration logWatcherConfiguration;
  private final ObjectMapper objectMapper;
  
  private final List<TailMetadataListener> listeners;
  
  @Inject
  public FileBasedSimpleStore(SingularityRunnerBaseConfiguration baseConfiguration, SingularityLogWatcherConfiguration logWatcherConfiguration, @Named(SingularityRunnerBaseModule.JSON_MAPPER) ObjectMapper objectMapper) {
    super(logWatcherConfiguration.getPollMillis(), baseConfiguration.getLogMetadataDirectory(), Arrays.asList(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY));
    this.logWatcherConfiguration = logWatcherConfiguration;
    this.baseConfiguration = baseConfiguration;
    this.objectMapper = objectMapper;
    
    this.listeners = Lists.newArrayList();
  }
  
  @Override
  public void start() {
    try {
      super.watch();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private boolean isMetadataFile(Path filename) {
    if (!filename.toString().endsWith(baseConfiguration.getLogMetadataSuffix())) {
      LOG.trace("Ignoring a file {} without {} suffix", filename, baseConfiguration.getLogMetadataSuffix());
      return false;
    }
    
    return true;
  }
  
  @Override
  protected boolean processEvent(Kind<?> kind, Path filename) throws IOException {
    if (!isMetadataFile(filename)) {
      return false;
    }
    
    Optional<TailMetadata> tail = read(baseConfiguration.getLogMetadataDirectory().resolve(filename).toAbsolutePath());
      
    if (!tail.isPresent()) {
      return false;
    }
    
    synchronized (listeners) {
      for (TailMetadataListener listener : listeners) {
        listener.tailChanged(tail.get());
      }
    }
    return true;
  }

  private void delete(Path path) throws IOException {
    boolean deleted = false;
    
    try {
      deleted = Files.deleteIfExists(path);
    } finally {
      LOG.trace("Deleted {} : {}", path, deleted);
    }
  }
  
  @Override
  public void markConsumed(TailMetadata tail) throws StoreException {
    Path tailMetadataPath = baseConfiguration.getTailMetadataPath(tail);
    Path storePath = getStorePath(tail);
    
    try {
      delete(tailMetadataPath);
      delete(storePath);
    } catch (IOException ioe) {
      throw new StoreException(String.format("Couldn't delete files %s and %s", tailMetadataPath, storePath), ioe);
    }
  }

  @Override
  public void savePosition(TailMetadata tail, long position) throws StoreException {
    Path storePath = getStorePath(tail);
    
    try {
      Files.write(storePath, JavaUtils.toBytes(Long.toString(position)), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    } catch (IOException e) {
      throw new StoreException("Couldn't write to " + storePath, e);
    }
  }
  
  private Path getStorePath(TailMetadata tail) {
    return logWatcherConfiguration.getStoreDirectory().resolve(Paths.get(tail.getFilenameKey() + logWatcherConfiguration.getStoreSuffix()));
  }
  
  @Override
  public Optional<Long> getPosition(TailMetadata tail) throws StoreException {
    Path storePath = getStorePath(tail);
    
    if (!Files.exists(storePath)) {
      return Optional.absent();
    }
    
    try {
      return Optional.of(Long.parseLong(JavaUtils.toString(Files.readAllBytes(storePath))));
    } catch (IOException e) {
      throw new StoreException("Couldn't read " + storePath, e);
    }
  }
  
  private Optional<TailMetadata> read(Path file) throws IOException {
    byte[] bytes = Files.readAllBytes(file);
    
    try {
      TailMetadata tail = objectMapper.readValue(bytes, TailMetadata.class);
      return Optional.of(tail);
    } catch (IOException e) {
      LOG.warn("File {} is not a valid TailMetadata", file, e);
    }
    
    return Optional.absent();
  }

  @Override
  public List<TailMetadata> getTails() {
    try {
      final List<TailMetadata> tails = Lists.newArrayList();
      
      for (Path file : JavaUtils.iterable(baseConfiguration.getLogMetadataDirectory())) {
        if (!isMetadataFile(file)) {
          continue;
        }
        
        Optional<TailMetadata> maybeTail = read(file);
        if (!maybeTail.isPresent()) {
          LOG.warn("File {} didn't contain TailMetadata", file);
          continue;
        }
        
        final TailMetadata tail = maybeTail.get();
        if (tails.contains(tail)) {
          LOG.warn("File {} contains a duplicate tail {}", file,tail);
          continue;
        }
        tails.add(tail);
      }
   
      return tails;
    } catch (IOException e) {
      throw new StoreException(e);
    }
  }

  @Override
  public void registerListener(TailMetadataListener listener) {
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  @Override
  public void removeListener(TailMetadataListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

}
