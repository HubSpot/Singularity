package com.hubspot.singularity.s3.base;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jets3t.service.S3Service;
import org.jets3t.service.model.S3Object;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hubspot.deploy.S3Artifact;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;

public class S3ArtifactChunkDownloader implements Callable<Path> {

  private final SingularityS3Configuration configuration;
  private final S3Service s3;
  private final S3Artifact s3Artifact;
  private final Path downloadTo;
  private final int chunk;
  private final long chunkSize;
  private final long length;
  private final Logger log;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;

  private int retryNum;

  public S3ArtifactChunkDownloader(SingularityS3Configuration configuration, Logger log, S3Service s3, S3Artifact s3Artifact, Path downloadTo, int chunk, long chunkSize, long length, SingularityRunnerExceptionNotifier exceptionNotifier) {
    this.configuration = configuration;
    this.log = log;
    this.s3 = s3;
    this.s3Artifact = s3Artifact;
    this.downloadTo = downloadTo;
    this.chunk = chunk;
    this.chunkSize = chunkSize;
    this.length = length;
    this.exceptionNotifier = exceptionNotifier;
  }

  @Override
  public Path call() throws Exception {
    final long start = System.currentTimeMillis();

    final ExecutorService chunkExecutorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("S3ArtifactDownloaderChunk-" + chunk + "-Thread-%d").build());

    try {
      while (retryNum <= configuration.getS3ChunkRetries()) {
        final long timeout = System.currentTimeMillis();

        final Future<Path> future = chunkExecutorService.submit(createDownloader(retryNum));

        try {
          return future.get(configuration.getS3ChunkDownloadTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
          log.error("Chunk {} (retry {}) for {} timed out after {} - total duration {}", chunk, retryNum, s3Artifact.getFilename(), JavaUtils.duration(timeout), JavaUtils.duration(start));
          future.cancel(true);
          if (retryNum == configuration.getS3ChunkRetries()) {
            exceptionNotifier.notify("Timeout downloading chunk", te, ImmutableMap.of("filename", s3Artifact.getFilename(), "chunk", Integer.toString(chunk), "retry", Integer.toString(retryNum)));
          }
        } catch (InterruptedException ie) {
          log.warn("Chunk {} (retry {}) for {} interrupted", chunk, retryNum, s3Artifact.getFilename());
          exceptionNotifier.notify("Interrupted during download", ie, ImmutableMap.of("filename", s3Artifact.getFilename(), "chunk", Integer.toString(chunk), "retry", Integer.toString(retryNum)));
        } catch (Throwable t) {
          log.error("Error while downloading chunk {} (retry {}) for {}", chunk, retryNum, s3Artifact.getFilename(), t);
          exceptionNotifier.notify(String.format("Error downloading chunk (%s)", t.getMessage()), t, ImmutableMap.of("filename", s3Artifact.getFilename(), "chunk", Integer.toString(chunk), "retry", Integer.toString(retryNum)));
        }

        retryNum++;
      }

      throw new IllegalStateException(String.format("Chunk %s for %s failed to download after %s tries", chunk, s3Artifact.getFilename(), retryNum + 1));
    } finally {
      chunkExecutorService.shutdownNow();
    }
  }

  private Callable<Path> createDownloader(final int retryNum) {
    return new Callable<Path>() {
      public Path call() throws Exception {
        final Path chunkPath = (chunk == 0) ? downloadTo : Paths.get(downloadTo + "_" + chunk + "_" + retryNum);
        chunkPath.toFile().deleteOnExit();

        final long startTime = System.currentTimeMillis();

        final long byteRangeStart = chunk * chunkSize;
        final long byteRangeEnd = Math.min((chunk + 1) * chunkSize - 1, length);

        log.info("Downloading {} - chunk {} (retry {}) ({}-{}) to {}", s3Artifact.getFilename(), chunk, retryNum, byteRangeStart, byteRangeEnd, chunkPath);

        S3Object fetchedObject = s3.getObject(s3Artifact.getS3Bucket(), s3Artifact.getS3ObjectKey(), null, null, null, null, byteRangeStart, byteRangeEnd);

        try (InputStream is = fetchedObject.getDataInputStream()) {
          Files.copy(is, chunkPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Finished downloading chunk {} (retry {}) of {} ({} bytes) in {}", chunk, retryNum, s3Artifact.getFilename(), byteRangeEnd - byteRangeStart, JavaUtils.duration(startTime));

        return chunkPath;
      };
    };
  }

}
