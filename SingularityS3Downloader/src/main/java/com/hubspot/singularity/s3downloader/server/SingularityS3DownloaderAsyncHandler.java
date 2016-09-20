package com.hubspot.singularity.s3downloader.server;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer.Context;
import com.google.common.collect.ImmutableMap;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.s3.base.ArtifactDownloadRequest;
import com.hubspot.singularity.s3.base.ArtifactManager;
import com.hubspot.singularity.s3downloader.SingularityS3DownloaderMetrics;

public class SingularityS3DownloaderAsyncHandler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityS3DownloaderAsyncHandler.class);

  private final ArtifactDownloadRequest artifactDownloadRequest;
  private final Continuation continuation;
  private final ArtifactManager artifactManager;
  private final long start;
  private final SingularityS3DownloaderMetrics metrics;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;

  public SingularityS3DownloaderAsyncHandler(ArtifactManager artifactManager, ArtifactDownloadRequest artifactDownloadRequest, Continuation continuation, SingularityS3DownloaderMetrics metrics, SingularityRunnerExceptionNotifier exceptionNotifier) {
    this.artifactManager = artifactManager;
    this.artifactDownloadRequest = artifactDownloadRequest;
    this.continuation = continuation;
    this.metrics = metrics;
    this.start = System.currentTimeMillis();
    this.exceptionNotifier = exceptionNotifier;
  }

  private void download() throws Exception {
    LOG.info("Beginning download {} after {}", artifactDownloadRequest, JavaUtils.duration(start));

    if (continuation.isExpired()) {
      LOG.info("Continuation expired for {}, aborting...", artifactDownloadRequest.getTargetDirectory());
      return;
    }

    final Path fetched = artifactManager.fetch(artifactDownloadRequest.getS3Artifact());
    final Path targetDirectory = Paths.get(artifactDownloadRequest.getTargetDirectory());

    if (continuation.isExpired()) {
      LOG.info("Continuation expired for {} after download, aborting...", artifactDownloadRequest.getTargetDirectory());
      return;
    }

    if (Objects.toString(fetched.getFileName()).endsWith(".tar.gz")) {
      artifactManager.untar(fetched, targetDirectory);
    } else {
      artifactManager.copy(fetched, targetDirectory, artifactDownloadRequest.getS3Artifact().getFilename());
    }

    LOG.info("Finishing request {} after {}", artifactDownloadRequest.getTargetDirectory(), JavaUtils.duration(start));

    getResponse().getOutputStream().close();
  }

  private HttpServletResponse getResponse() {
    return (HttpServletResponse) continuation.getServletResponse();
  }

  @Override
  public void run() {
    try (final Context context = metrics.getDownloadTimer().time()) {
      download();
    } catch (Throwable t) {
      metrics.getServerErrorsMeter().mark();
      LOG.error("While handling {}", artifactDownloadRequest.getTargetDirectory(), t);
      exceptionNotifier.notify(String.format("Error handling download (%s)", t.getMessage()), t, ImmutableMap.of("s3Bucket", artifactDownloadRequest.getS3Artifact().getS3Bucket(), "s3Key", artifactDownloadRequest.getS3Artifact().getS3ObjectKey(), "targetDirectory", artifactDownloadRequest.getTargetDirectory()));
      try {
        getResponse().sendError(500);
      } catch (Throwable t2) {
        LOG.error("While sending error for {}", artifactDownloadRequest.getTargetDirectory(), t2);
      }
    } finally {
      continuation.complete();
    }
  }

}
