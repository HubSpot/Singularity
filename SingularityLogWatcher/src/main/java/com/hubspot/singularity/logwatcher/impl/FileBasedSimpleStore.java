package com.hubspot.singularity.logwatcher.impl;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.Arrays;
import java.util.Iterator;
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
import com.hubspot.singularity.logwatcher.TailMetadata;
import com.hubspot.singularity.logwatcher.TailMetadataListener;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;

public class FileBasedSimpleStore extends WatchServiceHelper implements SimpleStore {

  private final static Logger LOG = LoggerFactory.getLogger(FileBasedSimpleStore.class);

  private final SingularityLogWatcherConfiguration configuration;
  private final ObjectMapper objectMapper;
  
  private final List<TailMetadataListener> listeners;
  
  @Inject
  public FileBasedSimpleStore(SingularityLogWatcherConfiguration configuration, @Named(SingularityRunnerBaseModule.JSON_MAPPER) ObjectMapper objectMapper) {
    super(configuration, configuration.getMetadataDirectory(), Arrays.asList(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY));
    this.configuration = configuration;
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
    if (!filename.toString().endsWith(configuration.getMetadataSuffix())) {
      LOG.trace("Ignoring a file {} without {} suffix", filename, configuration.getMetadataSuffix());
      return false;
    }
    
    return true;
  }
  
  @Override
  protected void processEvent(Kind<?> kind, Path filename) throws IOException {
    if (!isMetadataFile(filename)) {
      return;
    }
    
    Optional<TailMetadata> tail = read(filename.toAbsolutePath());
      
    if (tail.isPresent()) {
      synchronized (listeners) {
        for (TailMetadataListener listener : listeners) {
          listener.tailChanged(tail.get());
        }
      }
    }
  }

  @Override
  public void markConsumed(TailMetadata tail) throws StoreException {
    Path storePath = getStorePath(tail);
    Path tailMetadataPath = getTailMetadataPath(tail);
    
    try {
      Files.deleteIfExists(storePath);
      Files.deleteIfExists(tailMetadataPath);
    } catch (IOException ioe) {
      throw new StoreException("Couldn't delete files", ioe);
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
  
  private Path getTailMetadataPath(TailMetadata tail) {
    return Paths.get(tail.getFilename() + configuration.getMetadataSuffix());
  }
  
  private Path getStorePath(TailMetadata tail) {
    return Paths.get(tail.getFilename() + configuration.getStoreSuffix());
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
      final DirectoryStream<Path> dirStream = Files.newDirectoryStream(configuration.getMetadataDirectory());
      final Iterator<Path> iterator = dirStream.iterator(); // are you fucking kidding me java7?
      
      while (iterator.hasNext()) {
        Path file = iterator.next();
       
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
