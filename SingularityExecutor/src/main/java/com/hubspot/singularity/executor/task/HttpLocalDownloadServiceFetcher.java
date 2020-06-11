package com.hubspot.singularity.executor.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hubspot.deploy.S3Artifact;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.s3.base.ArtifactDownloadRequest;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class HttpLocalDownloadServiceFetcher implements LocalDownloadServiceFetcher {
  private static final String LOCAL_DOWNLOAD_STRING_FORMAT = "http://localhost:%s%s";

  private final AsyncHttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final SingularityExecutorConfiguration executorConfiguration;
  private final String localDownloadUri;

  public HttpLocalDownloadServiceFetcher(
    AsyncHttpClient httpClient,
    ObjectMapper objectMapper,
    SingularityExecutorConfiguration executorConfiguration,
    SingularityS3Configuration s3Configuration
  ) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.executorConfiguration = executorConfiguration;
    this.localDownloadUri =
      String.format(
        LOCAL_DOWNLOAD_STRING_FORMAT,
        s3Configuration.getLocalDownloadHttpPort(),
        s3Configuration.getLocalDownloadPath()
      );
  }

  @Override
  public String getDownloadPath() {
    return localDownloadUri;
  }

  @Override
  public void downloadFiles(
    List<? extends S3Artifact> s3Artifacts,
    SingularityExecutorTask task
  )
    throws InterruptedException {
    final List<FutureHolder> futures = Lists.newArrayListWithCapacity(s3Artifacts.size());

    for (S3Artifact s3Artifact : s3Artifacts) {
      String destination = task
        .getArtifactPath(s3Artifact, task.getTaskDefinition().getTaskDirectoryPath())
        .toString();
      ArtifactDownloadRequest artifactDownloadRequest = new ArtifactDownloadRequest(
        destination,
        s3Artifact,
        Optional.of(executorConfiguration.getLocalDownloadServiceTimeoutMillis())
      );

      task
        .getLog()
        .debug("Requesting {} from {}", artifactDownloadRequest, localDownloadUri);

      BoundRequestBuilder postRequestBldr = httpClient.preparePost(localDownloadUri);

      try {
        postRequestBldr.setBody(objectMapper.writeValueAsBytes(artifactDownloadRequest));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }

      try {
        ListenableFuture<Response> future = httpClient.executeRequest(
          postRequestBldr.build()
        );

        futures.add(new FutureHolder(future, System.currentTimeMillis(), s3Artifact));
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }

    for (FutureHolder future : futures) {
      Response response = future.getReponse();

      task
        .getLog()
        .debug(
          "Future for {} got status code {} after {}",
          future.s3Artifact.getName(),
          response.getStatusCode(),
          JavaUtils.duration(future.start)
        );

      if (response.getStatusCode() != 200) {
        throw new IllegalStateException("Got status code:" + response.getStatusCode());
      }
    }
  }

  private static class FutureHolder {
    private final ListenableFuture<Response> future;
    private final long start;
    private final S3Artifact s3Artifact;

    public FutureHolder(
      ListenableFuture<Response> future,
      long start,
      S3Artifact s3Artifact
    ) {
      this.future = future;
      this.start = start;
      this.s3Artifact = s3Artifact;
    }

    public Response getReponse() throws InterruptedException {
      try {
        return future.get();
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
