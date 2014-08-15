package com.hubspot.singularity.s3downloader.server;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.s3.base.ArtifactDownloadRequest;
import com.hubspot.singularity.s3.base.ArtifactManager;

public class SingularityS3DownloaderAsyncHandler implements Runnable {

  private final Logger LOG = LoggerFactory.getLogger(SingularityS3DownloaderAsyncHandler.class);

  private final ArtifactDownloadRequest artifactDownloadRequest;
  private final Continuation continuation;
  private final ArtifactManager artifactManager;
  private final long start;

  public SingularityS3DownloaderAsyncHandler(ArtifactManager artifactManager, ArtifactDownloadRequest artifactDownloadRequest, Continuation continuation) {
    this.artifactManager = artifactManager;
    this.artifactDownloadRequest = artifactDownloadRequest;
    this.continuation = continuation;
    this.start = System.currentTimeMillis();
  }

  private void download() throws Exception {
    LOG.info("Beginning download {} after {}", artifactDownloadRequest, JavaUtils.duration(start));

    if (continuation.isExpired()) {
      LOG.info("Continuation expired for {}, aborting...", artifactDownloadRequest);
      return;
    }

    final Path fetched = artifactManager.fetch(artifactDownloadRequest.getS3Artifact());
    final Path targetDirectory = Paths.get(artifactDownloadRequest.getTargetDirectory());

    if (continuation.isExpired()) {
      LOG.info("Continuation expired for {} after download, aborting...", artifactDownloadRequest);
      return;
    }

    if (fetched.getFileName().toString().endsWith(".tar.gz")) {
      artifactManager.untar(fetched, targetDirectory);
    } else {
      artifactManager.copy(fetched, targetDirectory);
    }

    LOG.info("Finishing request {} after {}", artifactDownloadRequest, JavaUtils.duration(start));

    getResponse().getOutputStream().close();
  }

  private HttpServletResponse getResponse() {
    return (HttpServletResponse) continuation.getServletResponse();
  }

  @Override
  public void run() {
    try {
      download();
    } catch (Throwable t) {
      LOG.error("While handling {}", artifactDownloadRequest, t);
      try {
        getResponse().sendError(500);
      } catch (Throwable t2) {
        LOG.error("While sending error", t2);
      }
    } finally {
      continuation.complete();
    }
  }


}
