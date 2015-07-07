package com.hubspot.singularity.s3downloader.server;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer.Context;
import com.hubspot.mesos.JavaUtils;
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

  public SingularityS3DownloaderAsyncHandler(ArtifactManager artifactManager, ArtifactDownloadRequest artifactDownloadRequest, Continuation continuation, SingularityS3DownloaderMetrics metrics) {
    this.artifactManager = artifactManager;
    this.artifactDownloadRequest = artifactDownloadRequest;
    this.continuation = continuation;
    this.metrics = metrics;
    this.start = System.currentTimeMillis();
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

    if (fetched.getFileName().toString().endsWith(".tar.gz")) {
      artifactManager.untar(fetched, targetDirectory);
    } else {
      artifactManager.copy(fetched, targetDirectory);
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