package com.hubspot.singularity.s3uploader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.WatchServiceHelper;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderConfiguration;

public class SingularityS3UploaderDriver extends WatchServiceHelper {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityS3UploaderDriver.class);

  private final SingularityS3UploaderConfiguration configuration;
  private final ScheduledExecutorService executorService;
  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityS3UploaderDriver(SingularityS3UploaderConfiguration configuration, @Named(SingularityRunnerBaseModule.JSON_MAPPER) ObjectMapper objectMapper) {
    super(configuration.getPollForShutDownMillis(), configuration.getS3MetadataDirectory(), ImmutableList.of(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE));
  
    this.configuration = configuration;
    this.objectMapper = objectMapper;
    
    this.executorService = Executors.newScheduledThreadPool(configuration.getExecutorCoreThreads(), new ThreadFactoryBuilder().setNameFormat("SingularityS3Uploader-%d").build());
  }

  @Override
  protected boolean processEvent(Kind<?> kind, Path filename) throws IOException {
    if (!isS3MetadataFile(filename)) {
      return false;
    }
    
    if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
      // DELETE .. stop processing that mofo
    } else if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
      if (optionallyProcessNewS3Metadata(filename)) {
        return true;
      }
    }
    
    return false;
  }
  
  private boolean optionallyProcessNewS3Metadata(Path filename) throws IOException {
    Optional<S3UploadMetadata> metadata = readS3UploadMetadata(filename);
    
    if (!metadata.isPresent()) {
      return false;
    }
    
    this.executorService.schedule(new Runnable() {
      
      @Override
      public void run() {
        // TODO build uploader and do it.. 
      }
    }, 1L, TimeUnit.SECONDS);
    
    return true;    
  }
  
  private Optional<S3UploadMetadata> readS3UploadMetadata(Path filename) throws IOException {
    byte[] s3MetadataBytes = Files.readAllBytes(configuration.getS3MetadataDirectory().resolve(filename));
    
    LOG.trace("Read {} bytes from {}", s3MetadataBytes.length, filename);
    
    try {
      return Optional.of(objectMapper.readValue(s3MetadataBytes, S3UploadMetadata.class));
    } catch (Throwable t) {
      LOG.warn("File {} was not a valid s3 metadata", filename, t);
      return Optional.absent();
    }
  }
  
  private boolean isS3MetadataFile(Path filename) {
    if (!filename.toString().endsWith(configuration.getS3MetadataSuffix())) {
      LOG.trace("Ignoring a file {} without {} suffix", filename, configuration.getS3MetadataSuffix());
      return false;
    }
    
    return true;
  }

}
