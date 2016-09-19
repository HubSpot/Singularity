package com.hubspot.singularity.s3.base;

import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.security.AWSCredentials;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hubspot.deploy.S3Artifact;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;

public class S3ArtifactDownloader {

  private final Logger log;
  private final SingularityS3Configuration configuration;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;

  public S3ArtifactDownloader(SingularityS3Configuration configuration, Logger log, SingularityRunnerExceptionNotifier exceptionNotifier) {
    this.configuration = configuration;
    this.log = log;
    this.exceptionNotifier = exceptionNotifier;
  }

  public void download(S3Artifact s3Artifact, Path downloadTo) {
    final long start = System.currentTimeMillis();
    boolean success = false;

    try {
      downloadThrows(s3Artifact, downloadTo);
      success = true;
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    } finally {
      log.info("S3 Download {}/{} finished {} after {}", s3Artifact.getS3Bucket(), s3Artifact.getS3ObjectKey(), success ? "successfully" : "with error", JavaUtils.duration(start));
    }
  }

  private AWSCredentials getCredentialsForBucket(String bucketName) {
    if (configuration.getS3BucketCredentials().containsKey(bucketName)) {
      return configuration.getS3BucketCredentials().get(bucketName).toAWSCredentials();
    }

    return new AWSCredentials(configuration.getS3AccessKey().get(), configuration.getS3SecretKey().get());
  }

  private void downloadThrows(final S3Artifact s3Artifact, final Path downloadTo) throws Exception {
    log.info("Downloading {}", s3Artifact);

    Jets3tProperties jets3tProperties = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME);
    jets3tProperties.setProperty("httpclient.socket-timeout-ms", Long.toString(configuration.getS3ChunkDownloadTimeoutMillis()));

    final S3Service s3 = new RestS3Service(getCredentialsForBucket(s3Artifact.getS3Bucket()), null, null, jets3tProperties);

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

    final long chunkSize = length / numChunks + (length % numChunks);

    log.info("Downloading {}/{} in {} chunks of {} bytes to {}", s3Artifact.getS3Bucket(), s3Artifact.getS3ObjectKey(), numChunks, chunkSize, downloadTo);

    final ExecutorService chunkExecutorService = Executors.newFixedThreadPool(numChunks, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("S3ArtifactDownloaderChunkThread-%d").build());
    final List<Future<Path>> futures = Lists.newArrayListWithCapacity(numChunks);

    for (int chunk = 0; chunk < numChunks; chunk++) {
      futures.add(chunkExecutorService.submit(new S3ArtifactChunkDownloader(configuration, log, s3, s3Artifact, downloadTo, chunk, chunkSize, length, exceptionNotifier)));
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

      if (!handleChunk(s3Artifact, future, downloadTo, chunk, start, remainingMillis)) {
        failed = true;
      }

      remainingMillis -= (System.currentTimeMillis() - start);
    }

    chunkExecutorService.shutdownNow();

    Preconditions.checkState(!failed, "Downloading %s/%s failed", s3Artifact.getS3Bucket(), s3Artifact.getS3ObjectKey());
  }

  private boolean handleChunk(S3Artifact s3Artifact, Future<Path> future, Path downloadTo, int chunk, long start, long remainingMillis) {
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
      log.error("Chunk {} for {} timed out after {} - had {} remaining", chunk, s3Artifact.getFilename(), JavaUtils.duration(start), JavaUtils.durationFromMillis(remainingMillis));
      future.cancel(true);
      exceptionNotifier.notify("TimeoutException during download", te, ImmutableMap.of("filename", s3Artifact.getFilename(), "chunk", Integer.toString(chunk)));
    } catch (Throwable t) {
      log.error("Error while handling chunk {} for {}", chunk, s3Artifact.getFilename(), t);
      exceptionNotifier.notify(String.format("Error handling chunk (%s)", t.getMessage()), t, ImmutableMap.of("filename", s3Artifact.getFilename(), "chunk", Integer.toString(chunk)));
    }

    return false;
  }

  private void combineChunk(Path downloadTo, Path path) throws Exception {
    final long start = System.currentTimeMillis();
    long bytes = 0;

    log.info("Writing {} to {}", path, downloadTo);

    try (WritableByteChannel wbs = Files.newByteChannel(downloadTo, EnumSet.of(StandardOpenOption.APPEND, StandardOpenOption.WRITE))) {
      try (FileChannel readChannel = FileChannel.open(path, EnumSet.of(StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE))) {
        bytes = readChannel.size();
        readChannel.transferTo(0, bytes, wbs);
      }
    }

    log.info("Finished writing {} bytes in {}", bytes, JavaUtils.duration(start));
  }

}
