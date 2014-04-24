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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
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
    super(configuration, Paths.get("lol"), Arrays.asList(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE));
    this.configuration = configuration;
    this.objectMapper = objectMapper;
    
    this.listeners = Lists.newArrayList();
  }
  
  @Override
  protected void processEvent(Kind<?> kind, Path filename) throws IOException {
    
  }

  @Override
  public void markConsumed(TailMetadata tail) throws StoreException {
    Path tailNotifyPath = getTailNotifyPath(tail);
    Path tailNotifyDonePath = getTailNotifyDonePath(tail);
    Path storePath = getStorePath(tail);
    
    try {
      Files.deleteIfExists(tailNotifyPath);
      Files.deleteIfExists(tailNotifyDonePath);
      Files.deleteIfExists(storePath);
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
  
  private Path getTailNotifyPath(TailMetadata tail) {
    return null;
  }
  
  private Path getTailNotifyDonePath(TailMetadata tail) {
    return null;
  }
  
  private Path getStorePath(TailMetadata tail) {
    return null;
  }
  
  private Path getTailNotifyDirectory() {
    return null;
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
      throw new StoreException("Coudln't read " + storePath, e);
    }
  }

  @Override
  public List<TailMetadata> getTails() {
    try {
      final List<TailMetadata> tails = Lists.newArrayList();
      final DirectoryStream<Path> dirStream = Files.newDirectoryStream(getTailNotifyDirectory());
      
      while (dirStream.iterator().hasNext()) {
        Path file = dirStream.iterator().next();
       
        byte[] bytes = Files.readAllBytes(file);
        
        try {
          TailMetadata tail = objectMapper.readValue(bytes, TailMetadata.class);
          tails.add(tail);
        } catch (IOException e) {
          LOG.warn("File {} is not a valid TailMetadata", file, e);
          continue;
        }
        
      }
   
      return tails;
    } catch (IOException e) {
      throw new StoreException(e);
    }
  }

  @Override
  public void registerListener(TailMetadataListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(TailMetadataListener listener) {
    listeners.remove(listener);
  }

}
