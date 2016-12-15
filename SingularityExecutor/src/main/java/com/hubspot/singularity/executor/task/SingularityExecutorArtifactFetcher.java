package com.hubspot.singularity.executor.task;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.deploy.EmbeddedArtifact;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.RemoteArtifact;
import com.hubspot.deploy.S3Artifact;
import com.hubspot.deploy.S3ArtifactSignature;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.config.SingularityExecutorModule;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseConfiguration;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.s3.base.ArtifactDownloadRequest;
import com.hubspot.singularity.s3.base.ArtifactManager;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

public class SingularityExecutorArtifactFetcher {

  private static final String LOCAL_DOWNLOAD_STRING_FORMAT = "http://localhost:%s%s";

  private final AsyncHttpClient localDownloadHttpClient;
  private final String localDownloadUri;
  private final SingularityExecutorConfiguration executorConfiguration;
  private final SingularityS3Configuration s3Configuration;
  private final ObjectMapper objectMapper;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;
  private final SingularityRunnerBaseConfiguration runnerBaseConfiguration;

  @Inject
  public SingularityExecutorArtifactFetcher(@Named(SingularityExecutorModule.LOCAL_DOWNLOAD_HTTP_CLIENT) AsyncHttpClient localDownloadHttpClient, SingularityS3Configuration s3Configuration,
      SingularityExecutorConfiguration executorConfiguration, ObjectMapper objectMapper, SingularityRunnerExceptionNotifier exceptionNotifier, SingularityRunnerBaseConfiguration runnerBaseConfiguration) {
    this.localDownloadHttpClient = localDownloadHttpClient;
    this.executorConfiguration = executorConfiguration;
    this.s3Configuration = s3Configuration;
    this.objectMapper = objectMapper;
    this.exceptionNotifier = exceptionNotifier;
    this.runnerBaseConfiguration = runnerBaseConfiguration;

    this.localDownloadUri = String.format(LOCAL_DOWNLOAD_STRING_FORMAT, s3Configuration.getLocalDownloadHttpPort(), s3Configuration.getLocalDownloadPath());
  }

  public SingularityExecutorTaskArtifactFetcher buildTaskFetcher(ExecutorData executorData, SingularityExecutorTask task) {
    ArtifactManager artifactManager = new ArtifactManager(runnerBaseConfiguration, s3Configuration, task.getLog(), exceptionNotifier);

    return new SingularityExecutorTaskArtifactFetcher(artifactManager, executorData, task);
  }

  public class SingularityExecutorTaskArtifactFetcher {

    private final ArtifactManager artifactManager;
    private final ExecutorData executorData;
    private final SingularityExecutorTask task;

    private SingularityExecutorTaskArtifactFetcher(ArtifactManager artifactManager, ExecutorData executorData, SingularityExecutorTask task) {
      this.artifactManager = artifactManager;
      this.executorData = executorData;
      this.task = task;
    }

    public void cancel() {
      artifactManager.markKilled();
      artifactManager.signalKillToProcessIfActive();
    }

    public void fetchFiles() throws InterruptedException {
      extractFiles(task, artifactManager, executorData);

      boolean fetchS3ArtifactsLocally = true;

      final ImmutableList<S3Artifact> allS3Artifacts = ImmutableList.<S3Artifact>builder()
          .addAll(executorData.getS3Artifacts())
          .addAll(executorData.getS3ArtifactSignatures().or(Collections.<S3ArtifactSignature>emptyList()))
          .build();

      if (executorConfiguration.isUseLocalDownloadService() && !allS3Artifacts.isEmpty()) {
        final long start = System.currentTimeMillis();

        task.getLog().info("Fetching {} (S3) artifacts and {} (S3) artifact signatures from {}", executorData.getS3Artifacts().size(),
            executorData.getS3ArtifactSignatures().isPresent() ? executorData.getS3ArtifactSignatures().get().size() : 0, localDownloadUri);

        try {
          downloadFilesFromLocalDownloadService(allS3Artifacts, task);

          fetchS3ArtifactsLocally = false;

          task.getLog().info("Fetched {} (S3) artifacts and {} (S3) artifact signatures from local download service in {}", executorData.getS3Artifacts().size(),
              executorData.getS3ArtifactSignatures().isPresent() ? executorData.getS3ArtifactSignatures().get().size() : 0, JavaUtils.duration(start));
        } catch (InterruptedException ie) {
          task.getLog().warn("Interrupted while downloading S3 artifacts from local download service");
          throw ie;
        } catch (Throwable t) {
          task.getLog().error("Failed downloading S3 artifacts from local download service - falling back to in-task fetch", t);
        }
      }

      if (fetchS3ArtifactsLocally) {
        for (RemoteArtifact s3Artifact : allS3Artifacts) {
          downloadRemoteArtifact(s3Artifact, artifactManager, task);
        }
      }

      for (RemoteArtifact externalArtifact : executorData.getExternalArtifacts()) {
        downloadRemoteArtifact(externalArtifact, artifactManager, task);
      }
    }

    private void extractFiles(SingularityExecutorTask task, ArtifactManager artifactManager, ExecutorData executorData) {
      for (EmbeddedArtifact artifact : executorData.getEmbeddedArtifacts()) {
        artifactManager.extract(artifact, task.getArtifactPath(artifact, task.getTaskDefinition().getTaskDirectoryPath()));
      }
    }

    private class FutureHolder {

      private final ListenableFuture<Response> future;
      private final long start;
      private final S3Artifact s3Artifact;

      public FutureHolder(ListenableFuture<Response> future, long start, S3Artifact s3Artifact) {
        this.future = future;
        this.start = start;
        this.s3Artifact = s3Artifact;
      }

      public Response getReponse() throws InterruptedException {
        try {
          return future.get();
        } catch (ExecutionException e) {
          throw Throwables.propagate(e);
        }
      }

    }

    private void downloadFilesFromLocalDownloadService(List<? extends S3Artifact> s3Artifacts, SingularityExecutorTask task) throws InterruptedException {
      final List<FutureHolder> futures = Lists.newArrayListWithCapacity(s3Artifacts.size());

      for (S3Artifact s3Artifact : s3Artifacts) {
        String destination = task.getArtifactPath(s3Artifact, task.getTaskDefinition().getTaskDirectoryPath()).toString();
        ArtifactDownloadRequest artifactDownloadRequest = new ArtifactDownloadRequest(destination, s3Artifact,
            Optional.of(SingularityExecutorArtifactFetcher.this.executorConfiguration.getLocalDownloadServiceTimeoutMillis()));

        task.getLog().debug("Requesting {} from {}", artifactDownloadRequest, localDownloadUri);

        BoundRequestBuilder postRequestBldr = localDownloadHttpClient.preparePost(localDownloadUri);

        try {
          postRequestBldr.setBody(objectMapper.writeValueAsBytes(artifactDownloadRequest));
        } catch (JsonProcessingException e) {
          throw Throwables.propagate(e);
        }

        try {
          ListenableFuture<Response> future = localDownloadHttpClient.executeRequest(postRequestBldr.build());

          futures.add(new FutureHolder(future, System.currentTimeMillis(), s3Artifact));
        } catch (IOException ioe) {
          throw Throwables.propagate(ioe);
        }
      }

      for (FutureHolder future : futures) {
        Response response = future.getReponse();

        task.getLog().debug("Future for {} got status code {} after {}", future.s3Artifact.getName(), response.getStatusCode(), JavaUtils.duration(future.start));

        if (response.getStatusCode() != 200) {
          throw new IllegalStateException("Got status code:" + response.getStatusCode());
        }
      }
    }

    private void downloadRemoteArtifact(RemoteArtifact remoteArtifact, ArtifactManager artifactManager, SingularityExecutorTask task) {
      Path fetched = artifactManager.fetch(remoteArtifact);

      if (Objects.toString(fetched.getFileName()).endsWith(".tar.gz")) {
        artifactManager.untar(fetched, task.getArtifactPath(remoteArtifact, task.getTaskDefinition().getTaskDirectoryPath()));
      } else {
        artifactManager.copy(fetched, task.getArtifactPath(remoteArtifact, task.getTaskDefinition().getTaskAppDirectoryPath()), remoteArtifact.getFilename());
      }
    }

  }

}
