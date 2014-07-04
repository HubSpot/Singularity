package com.hubspot.singularity.executor;

import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.security.AWSCredentials;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hubspot.deploy.S3Artifact;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;

public class S3ArtifactDownloader {

  private final Logger log;
  private final SingularityExecutorConfiguration configuration;
  
  public S3ArtifactDownloader(SingularityExecutorConfiguration configuration, Logger log) {
    this.configuration = configuration;
    this.log = log;
  }
  
  public void download(S3Artifact s3Artifact, Path downloadTo) {
    try {
      downloadThrows(s3Artifact, downloadTo);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  private Callable<Path> buildChunkDownloader(final S3Service s3, final S3Artifact s3Artifact, final Path downloadTo, final int chunk, final long length) {
    return new Callable<Path>() {

      @Override
      public Path call() throws Exception {
        final Path chunkPath = (chunk == 0) ? downloadTo : Paths.get(downloadTo + "_" + chunk);
        
        final long startTime = System.currentTimeMillis();
        
        log.info("Downloading chunk {} to {}", chunk, chunkPath);
        
        final long byteRangeStart = chunk * configuration.getS3ChunkSize();
        final long byteRangeEnd = Math.min((chunk + 1) * configuration.getS3ChunkSize(), length);
        
        S3Object fetchedObject = s3.getObject(s3Artifact.getS3Bucket(), s3Artifact.getS3ObjectKey(), null, null, null, null, byteRangeStart, byteRangeEnd);
        
        Files.copy(fetchedObject.getDataInputStream(), chunkPath, StandardCopyOption.REPLACE_EXISTING);
      
        fetchedObject.getDataInputStream().close();
      
        log.info("Finished downloading chunk {} ({} bytes) in {}", chunk, byteRangeEnd - byteRangeStart, JavaUtils.duration(startTime));
        
        return chunkPath;
      }
      
    };
  }
  
  private void downloadThrows(final S3Artifact s3Artifact, final Path downloadTo) throws Exception {
    final S3Service s3 = new RestS3Service(new AWSCredentials(configuration.getS3AccessKey(), configuration.getS3SecretKey()));
  
    long length = 0;
    
    if (s3Artifact.getFilesize().isPresent()) {
      length = s3Artifact.getFilesize().get();
    } else {
      StorageObject details = s3.getObjectDetails(s3Artifact.getS3Bucket(), s3Artifact.getS3ObjectKey());
    
      Preconditions.checkNotNull(details, "Couldn't find object at %s/%s", s3Artifact.getS3Bucket(), s3Artifact.getS3ObjectKey());
      
      length = details.getContentLength();
    }
    
    int numChunks = (int) (length / configuration.getS3ChunkSize());
    
    if (length % configuration.getS3ChunkSize() > 0) {
      numChunks++;
    }
    
    final ExecutorService chunkExecutorService = Executors.newFixedThreadPool(numChunks, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("S3ArtifactDownloaderChunkThread-%d").build());
    final List<Future<Path>> futures = Lists.newArrayListWithCapacity(numChunks);
    
    for (int i = 0; i < numChunks; i++) {
      final int chunk = i;
      
      futures.add(chunkExecutorService.submit(buildChunkDownloader(s3, s3Artifact, downloadTo, chunk, length)));
    }
    
    long remainingMillis = configuration.getS3DownloadTimeoutMillis();
    boolean failed = false;
    
    for (int chunk = 0; chunk < numChunks; chunk++) {
      final Future<Path> future = futures.get(chunk);
      
      if (failed) {
        future.cancel(true);
        continue;
      }
      
      final long start = System.currentTimeMillis();
      
      if (!handleChunk(future, downloadTo, chunk, start, remainingMillis)) {
        failed = true;
      }
      
      remainingMillis -= (System.currentTimeMillis() - start);
    }
  
    chunkExecutorService.shutdown();
    
    Preconditions.checkState(!failed, "Downloading %s/%s failed", s3Artifact.getS3Bucket(), s3Artifact.getS3ObjectKey());
  }
  
  private boolean handleChunk(Future<Path> future, Path downloadTo, int chunk, long start, long remainingMillis) {
    if (remainingMillis <= 0) {
      remainingMillis = 1;
    }
    
    try {
      Path path = future.get(remainingMillis, TimeUnit.MILLISECONDS);
      
      if (chunk > 0) {
        combineChunk(downloadTo, path);
      }
      
      return true;
    } catch (TimeoutException te) {
      log.error("Chunk {} timed out after {} - had {} remaining", chunk, JavaUtils.duration(start), JavaUtils.durationFromMillis(remainingMillis));
      
      future.cancel(true);
    } catch (InterruptedException ie ) {
      log.warn("Chunk {} interrupted", chunk);
    } catch (Throwable t) {
      log.error("Error while downloading chunk {}", chunk, t);
    }

    return false;
  }
  
  private void combineChunk(Path downloadTo, Path path) throws Exception {
    final long start = System.currentTimeMillis();
    long bytes = 0;
    
    log.info("Writing {} to {}", path, downloadTo);
    
    try (WritableByteChannel wbs = Files.newByteChannel(downloadTo, EnumSet.of(StandardOpenOption.APPEND, StandardOpenOption.WRITE))) {
      try (FileChannel readChannel = FileChannel.open(path, StandardOpenOption.READ)) {
        bytes = readChannel.size();
        readChannel.transferTo(0, bytes, wbs);
      }
    }
    
    log.info("Finished writing {} bytes in {}", bytes, JavaUtils.duration(start));
  }
  
}
