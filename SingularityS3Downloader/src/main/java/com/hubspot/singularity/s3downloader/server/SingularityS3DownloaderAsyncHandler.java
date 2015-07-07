package com.hubspot.singularity.s3downloader.server;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer.Context;
import com.google.common.collect.Lists;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.s3.base.ArtifactDownloadRequest;
import com.hubspot.singularity.s3.base.ArtifactManager;
import com.hubspot.singularity.s3downloader.SingularityS3DownloaderMetrics;

public class SingularityS3DownloaderAsyncHandler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityS3DownloaderAsyncHandler.class);

  private final long start;

  private final ArtifactDownloadRequest artifactDownloadRequest;
  private final ArtifactManager artifactManager;
  private final SingularityS3DownloaderMetrics metrics;

  private final AtomicBoolean finished;
  private final Lock continuationsLock;
  private final List<Continuation> continuations;

  public SingularityS3DownloaderAsyncHandler(ArtifactManager artifactManager, ArtifactDownloadRequest artifactDownloadRequest, Continuation continuation, SingularityS3DownloaderMetrics metrics) {
    this.artifactManager = artifactManager;
    this.artifactDownloadRequest = artifactDownloadRequest;
    this.metrics = metrics;
    this.start = System.currentTimeMillis();

    this.finished = new AtomicBoolean(false);
    this.continuationsLock = new ReentrantLock();
    this.continuations = Lists.newArrayList(continuation);
  }

  public boolean addContinuation(Continuation continuation) {
    if (finished.get()) {
      return false;
    }

    try {
      continuationsLock.lock();

      if (finished.get()) {
        return false;
      }

      continuations.add(continuation);
    } finally {
      continuationsLock.unlock();
    }

    return true;
  }

  private boolean isExpired() {
    try {
      continuationsLock.lock();

      boolean isExpired = true;

      for (Continuation continuation : continuations) {
        if (!continuation.isExpired()) {
          isExpired = false;
        }
      }

      if (isExpired) {
        finished.set(true);
      }

      return isExpired;
    } finally {
      continuationsLock.unlock();
    }
  }

  private void closeContinuations() {
    for (Continuation continuation : continuations) {
      try {
        getResponse(continuation).getOutputStream().close();
      } catch (IOException e) {
        LOG.warn("Error closing continuation for {}", artifactDownloadRequest, e);
      }
    }
  }

  private HttpServletResponse getResponse(Continuation continuation) {
    return (HttpServletResponse) continuation.getServletResponse();
  }

  private void completeContinuations() {
    for (Continuation continuation : continuations) {
      continuation.complete();
    }
  }

  private void sendErrors() {
    for (Continuation continuation : continuations) {
      try {
        getResponse(continuation).getOutputStream().close();
      } catch (IOException e) {
        LOG.warn("Error sending error for {}", artifactDownloadRequest, e);
      }
    }
  }

  private void download() throws Exception {
    LOG.info("Beginning download {} after {}", artifactDownloadRequest, JavaUtils.duration(start));

    if (isExpired()) {
      LOG.info("Continuations expired for {}, aborting...", artifactDownloadRequest);
      return;
    }

    final Path fetched = artifactManager.fetch(artifactDownloadRequest.getS3Artifact());
    final Path targetDirectory = Paths.get(artifactDownloadRequest.getTargetDirectory());

    if (isExpired()) {
      LOG.info("Continuations expired for {} after download, aborting...", artifactDownloadRequest);
      return;
    }

    if (fetched.getFileName().toString().endsWith(".tar.gz")) {
      artifactManager.untar(fetched, targetDirectory);
    } else {
      artifactManager.copy(fetched, targetDirectory);
    }

    LOG.info("Finishing request {} after {}", artifactDownloadRequest, JavaUtils.duration(start));

    closeContinuations();
  }

  @Override
  public void run() {
    try (final Context context = metrics.getDownloadTimer().time()) {
      download();
      finished.set(true);
    } catch (Throwable t) {
      finished.set(true);
      metrics.getServerErrorsMeter().mark();
      LOG.error("While handling {}", artifactDownloadRequest, t);
      sendErrors();
    } finally {
      completeContinuations();
    }
  }
}
