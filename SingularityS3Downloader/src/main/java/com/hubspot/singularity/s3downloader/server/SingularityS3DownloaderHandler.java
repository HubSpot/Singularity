package com.hubspot.singularity.s3downloader.server;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.hubspot.singularity.s3.base.ArtifactDownloadRequest;
import com.hubspot.singularity.s3.base.ArtifactManager;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import com.hubspot.singularity.s3downloader.SingularityS3DownloaderMetrics;
import com.hubspot.singularity.s3downloader.config.SingularityS3DownloaderModule;

public class SingularityS3DownloaderHandler extends AbstractHandler {

  private final Logger LOG = LoggerFactory.getLogger(SingularityS3DownloaderHandler.class);

  private final SingularityS3Configuration s3Configuration;
  private final ObjectMapper objectMapper;
  private final Provider<ArtifactManager> artifactManagerProvider;
  private final ThreadPoolExecutor asyncDownloadService;
  private final SingularityS3DownloaderMetrics metrics;

  @Inject
  public SingularityS3DownloaderHandler(Provider<ArtifactManager> artifactManagerProvider, SingularityS3Configuration s3Configuration, ObjectMapper objectMapper,
      @Named(SingularityS3DownloaderModule.DOWNLOAD_EXECUTOR_SERVICE) ThreadPoolExecutor asyncDownloadService, SingularityS3DownloaderMetrics metrics) {
    this.artifactManagerProvider = artifactManagerProvider;
    this.s3Configuration = s3Configuration;
    this.objectMapper = objectMapper;
    this.asyncDownloadService = asyncDownloadService;
    this.metrics = metrics;
  }

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    metrics.getRequestsMeter().mark();

    if (!target.equals(s3Configuration.getLocalDownloadPath())) {
      metrics.getClientErrorsMeter().mark();
      response.sendError(404);
      return;
    }

    if (!request.getMethod().equalsIgnoreCase(HttpMethod.POST.name())) {
      metrics.getClientErrorsMeter().mark();
      response.sendError(405);
      return;
    }

    Optional<ArtifactDownloadRequest> artifactOptional = readDownloadRequest(request);

    if (!artifactOptional.isPresent()) {
      metrics.getClientErrorsMeter().mark();
      response.sendError(400);
      return;
    }

    Continuation continuation = ContinuationSupport.getContinuation(request);
    continuation.suspend(response);

    LOG.info("Queing handler for {} ({} active threads, {} queue size)", artifactOptional.get(), asyncDownloadService.getActiveCount(), asyncDownloadService.getQueue().size());

    SingularityS3DownloaderAsyncHandler asyncHandler = new SingularityS3DownloaderAsyncHandler(artifactManagerProvider.get(), artifactOptional.get(), continuation, metrics);

    asyncDownloadService.submit(asyncHandler);
  }

  private Optional<ArtifactDownloadRequest> readDownloadRequest(HttpServletRequest request) {
    try {
      return Optional.of(objectMapper.readValue(request.getInputStream(), ArtifactDownloadRequest.class));
    } catch (Throwable t) {
      return Optional.absent();
    }
  }


}
