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

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.s3.base.ArtifactDownloadRequest;
import com.hubspot.singularity.s3.base.ArtifactManager;
import com.hubspot.singularity.s3downloader.SingularityS3DownloaderMetrics;
import com.hubspot.singularity.s3downloader.config.SingularityS3DownloaderConfiguration;
import com.hubspot.singularity.s3downloader.config.SingularityS3DownloaderModule;

public class SingularityS3DownloaderCoordinator {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityS3DownloaderCoordinator.class);

  private final SingularityS3DownloaderConfiguration configuration;
  private final SingularityS3DownloaderMetrics metrics;
  private final Provider<ArtifactManager> artifactManagerProvider;
  private final ConcurrentMap<ArtifactDownloadRequest, SingularityS3DownloaderAsyncHandler> downloadRequestToHandler;
  private final ScheduledThreadPoolExecutor downloadJoinerService;
  private final ThreadPoolExecutor downloadService;
  private final ListeningExecutorService listeningDownloadWrapper;
  private final ExecutorService listeningResponseExecutorService;

  @Inject
  public SingularityS3DownloaderCoordinator(SingularityS3DownloaderConfiguration configuration, SingularityS3DownloaderMetrics metrics, Provider<ArtifactManager> artifactManagerProvider,
      @Named(SingularityS3DownloaderModule.ENQUEUE_EXECUTOR_SERVICE) ScheduledThreadPoolExecutor downloadJoinerService,
      @Named(SingularityS3DownloaderModule.DOWNLOAD_EXECUTOR_SERVICE) ThreadPoolExecutor downloadService) {
    this.configuration = configuration;
    this.metrics = metrics;
    this.artifactManagerProvider = artifactManagerProvider;
    this.downloadJoinerService = downloadJoinerService;
    this.downloadService = downloadService;
    this.downloadRequestToHandler = Maps.newConcurrentMap();
    this.listeningDownloadWrapper = MoreExecutors.listeningDecorator(downloadService);
    this.listeningResponseExecutorService = Executors.newCachedThreadPool();
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
      LOG.info("Re-enqueueing request for {}, waiting {}, ({} active, {} queue, {} max)", artifactDownloadRequest, JavaUtils.durationFromMillis(configuration.getMillisToWaitForReEnqueue()),
          downloadJoinerService.getActiveCount(), downloadJoinerService.getQueue().size(), configuration.getNumEnqueueThreads());

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
        LOG.info("Queing new downloader for {} ({} handlers, {} active threads, {} queue size, {} max)", artifactDownloadRequest, downloadRequestToHandler.size(),
            downloadService.getActiveCount(), downloadService.getQueue().size(), configuration.getNumDownloaderThreads());

        ListenableFuture<?> future = listeningDownloadWrapper.submit(newHandler);

        future.addListener(new Runnable() {
          @Override
          public void run() {
            downloadRequestToHandler.remove(artifactDownloadRequest);
          }
        }, listeningResponseExecutorService);
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
