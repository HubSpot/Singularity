package com.hubspot.singularity.s3downloader.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.s3.base.ArtifactDownloadRequest;
import com.hubspot.singularity.s3.base.ArtifactManager;
import com.hubspot.singularity.s3downloader.SingularityS3DownloaderMetrics;
import com.hubspot.singularity.s3downloader.config.SingularityS3DownloaderConfiguration;

public class SingularityS3DownloaderCoordinator {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityS3DownloaderCoordinator.class);

  private final SingularityS3DownloaderConfiguration configuration;
  private final SingularityS3DownloaderMetrics metrics;
  private final Provider<ArtifactManager> artifactManagerProvider;
  private final Map<ArtifactDownloadRequest, SingularityS3DownloaderAsyncHandler> downloadRequestToHandler;
  private final ScheduledExecutorService downloadJoinerService;
  private final ExecutorService downloadService;

  @Inject
  public SingularityS3DownloaderCoordinator(SingularityS3DownloaderConfiguration configuration, SingularityS3DownloaderMetrics metrics, Provider<ArtifactManager> artifactManagerProvider,
      Map<ArtifactDownloadRequest, SingularityS3DownloaderAsyncHandler> downloadRequestToHandler, ScheduledExecutorService downloadJoinerService, ExecutorService downloadService) {
    this.configuration = configuration;
    this.metrics = metrics;
    this.artifactManagerProvider = artifactManagerProvider;
    this.downloadRequestToHandler = downloadRequestToHandler;
    this.downloadJoinerService = downloadJoinerService;
    this.downloadService = downloadService;
  }

  public void register(final Continuation continuation, final ArtifactDownloadRequest artifactDownloadRequest) {
    final DownloadJoiner downloadJoiner = new DownloadJoiner(continuation, artifactDownloadRequest);

    downloadJoinerService.submit(downloadJoiner);
  }

  private class DownloadJoiner implements Runnable {

    private final long start;
    private final Continuation continuation;
    private final ArtifactDownloadRequest artifactDownloadRequest;

    public DownloadJoiner(Continuation continuation, ArtifactDownloadRequest artifactDownloadRequest) {
      this.continuation = continuation;
      this.artifactDownloadRequest = artifactDownloadRequest;
      this.start = System.currentTimeMillis();
    }

    private boolean addContinuation(SingularityS3DownloaderAsyncHandler existingHandler) {
      if (existingHandler.addContinuation(continuation)) {
        LOG.info("Added continuation to existing request after {} ({})", JavaUtils.duration(start), artifactDownloadRequest);
        return true;
      } else {
        LOG.info("Unable to join existing continuation for request (waiting {}) {}", JavaUtils.duration(start), artifactDownloadRequest);
        return false;
      }
    }

    private void reEnqueue() {
      downloadJoinerService.schedule(this, configuration.getMillisToWaitForReEnqueue(), TimeUnit.MILLISECONDS);
    }

    private boolean addDownloadRequest() {
      SingularityS3DownloaderAsyncHandler existingHandler = downloadRequestToHandler.get(artifactDownloadRequest);

      if (existingHandler != null) {
        return addContinuation(existingHandler);
      }

      SingularityS3DownloaderAsyncHandler newHandler = new SingularityS3DownloaderAsyncHandler(artifactManagerProvider.get(), artifactDownloadRequest, continuation, metrics);

      existingHandler = downloadRequestToHandler.putIfAbsent(artifactDownloadRequest, newHandler);

      if (existingHandler != null) {
        return addContinuation(existingHandler);
      } else {
        downloadService.submit(newHandler);
      }

      return true;
    }

    @Override
    public void run() {
      try {
        if (!addDownloadRequest()) {
          reEnqueue();
        }
      } catch (Throwable t) {
        LOG.error("While trying to enqueue for {}", artifactDownloadRequest, t);
        try {
          ((HttpServletResponse) continuation.getServletResponse()).sendError(500);
        } catch (IOException e) {
          LOG.error("Couldn't send error to continuation", e);
        } finally {
          continuation.complete();
        }
      }
    }

  }

}
