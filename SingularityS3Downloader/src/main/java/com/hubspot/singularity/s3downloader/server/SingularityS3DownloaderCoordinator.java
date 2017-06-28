package com.hubspot.singularity.s3downloader.server;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.hubspot.deploy.S3Artifact;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.s3.base.ArtifactDownloadRequest;
import com.hubspot.singularity.s3.base.ArtifactManager;
import com.hubspot.singularity.s3downloader.SingularityS3DownloaderMetrics;
import com.hubspot.singularity.s3downloader.config.SingularityS3DownloaderConfiguration;
import com.hubspot.singularity.s3downloader.config.SingularityS3DownloaderModule;

public class SingularityS3DownloaderCoordinator implements DownloadListener {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityS3DownloaderCoordinator.class);

  private final SingularityS3DownloaderConfiguration configuration;
  private final SingularityS3DownloaderMetrics metrics;
  private final Provider<ArtifactManager> artifactManagerProvider;
  private final ConcurrentMap<S3Artifact, SingularityS3DownloaderAsyncHandler> downloadRequestToHandler;
  private final ScheduledThreadPoolExecutor downloadJoinerService;
  private final ThreadPoolExecutor downloadService;
  private final ListeningExecutorService listeningDownloadWrapper;
  private final ExecutorService listeningResponseExecutorService;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityS3DownloaderCoordinator(SingularityS3DownloaderConfiguration configuration, SingularityS3DownloaderMetrics metrics, Provider<ArtifactManager> artifactManagerProvider,
      @Named(SingularityS3DownloaderModule.ENQUEUE_EXECUTOR_SERVICE) ScheduledThreadPoolExecutor downloadJoinerService,
      @Named(SingularityS3DownloaderModule.DOWNLOAD_EXECUTOR_SERVICE) ThreadPoolExecutor downloadService, SingularityRunnerExceptionNotifier exceptionNotifier) {
    this.configuration = configuration;
    this.metrics = metrics;
    this.artifactManagerProvider = artifactManagerProvider;
    this.downloadJoinerService = downloadJoinerService;
    this.downloadService = downloadService;
    this.downloadRequestToHandler = Maps.newConcurrentMap();
    this.listeningDownloadWrapper = MoreExecutors.listeningDecorator(downloadService);
    this.listeningResponseExecutorService = Executors.newCachedThreadPool();
    this.exceptionNotifier = exceptionNotifier;
  }

  public void register(final Continuation continuation, final ArtifactDownloadRequest artifactDownloadRequest) {
    final DownloadJoiner downloadJoiner = new DownloadJoiner(continuation, artifactDownloadRequest);

    downloadJoinerService.submit(downloadJoiner);
  }

  @Override
  public void notifyDownloadFinished(SingularityS3DownloaderAsyncHandler handler) {
    boolean removed = downloadRequestToHandler.remove(handler.getS3Artifact(), handler);
    LOG.debug("Handler for artifact {} finished download - removed {}", handler.getS3Artifact(), removed);
  }

  private class DownloadJoiner implements Runnable {

    private final long start;
    private final Continuation continuation;
    private final ArtifactDownloadRequest artifactDownloadRequest;

    private DownloadJoiner(Continuation continuation, ArtifactDownloadRequest artifactDownloadRequest) {
      this.continuation = continuation;
      this.artifactDownloadRequest = artifactDownloadRequest;
      this.start = System.currentTimeMillis();
    }

    private void reEnqueue() {
      LOG.debug("Re-enqueueing request for {}, waiting {}, ({} active, {} queue, {} max), total time {}", artifactDownloadRequest.getTargetDirectory(),
          JavaUtils.durationFromMillis(configuration.getMillisToWaitForReEnqueue()),
          downloadJoinerService.getActiveCount(),
          downloadJoinerService.getQueue().size(),
          configuration.getNumEnqueueThreads(),
          JavaUtils.duration(start));

      downloadJoinerService.schedule(this, configuration.getMillisToWaitForReEnqueue(), TimeUnit.MILLISECONDS);
    }

    private boolean addDownloadRequest() {
      SingularityS3DownloaderAsyncHandler existingHandler = downloadRequestToHandler.get(artifactDownloadRequest.getS3Artifact());

      if (existingHandler != null) {
        return false;
      }

      final SingularityS3DownloaderAsyncHandler newHandler = new SingularityS3DownloaderAsyncHandler(artifactManagerProvider.get(), artifactDownloadRequest, continuation, metrics, exceptionNotifier,
          SingularityS3DownloaderCoordinator.this);

      existingHandler = downloadRequestToHandler.putIfAbsent(artifactDownloadRequest.getS3Artifact(), newHandler);

      if (existingHandler != null) {
        return false;
      }

      LOG.info("Queing new downloader for {} ({} handlers, {} active threads, {} queue size, {} max) after {}", artifactDownloadRequest, downloadRequestToHandler.size(),
          downloadService.getActiveCount(), downloadService.getQueue().size(), configuration.getNumDownloaderThreads(), JavaUtils.duration(start));

      ListenableFuture<?> future = listeningDownloadWrapper.submit(newHandler);

      future.addListener(new Runnable() {
        @Override
        public void run() {
          notifyDownloadFinished(newHandler);
        }
      }, listeningResponseExecutorService);

      return true;
    }

    private void handleContinuationExpired() {
      try {
        LOG.info("Continuation expired for {} after {} - returning 500", artifactDownloadRequest, JavaUtils.duration(start));
        ((HttpServletResponse) continuation.getServletResponse()).sendError(500, "Hit client timeout");
      } catch (Throwable t) {
        LOG.warn("{} while sending error after continuation for {}", t.getClass().getSimpleName(), artifactDownloadRequest.getTargetDirectory());
      } finally {
        continuation.complete();
      }
    }

    @Override
    public void run() {
      try {
        if (!addDownloadRequest()) {
          if (continuation.isExpired()) {
            handleContinuationExpired();
            return;
          }
          reEnqueue();
        }
      } catch (Throwable t) {
        LOG.error("While trying to enqueue {}", artifactDownloadRequest.getTargetDirectory(), t);
        exceptionNotifier.notify(String.format("Error enqueuing download (%s)", t.getMessage()), t, ImmutableMap.of("targetDirectory", artifactDownloadRequest.getTargetDirectory()));
        try {
          ((HttpServletResponse) continuation.getServletResponse()).sendError(500);
        } catch (IOException e) {
          LOG.error("Couldn't send error for {}", artifactDownloadRequest.getTargetDirectory(), e);
        } finally {
          continuation.complete();
        }
      }
    }

  }

}
