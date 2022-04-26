package com.hubspot.singularity.executor.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hubspot.deploy.EmbeddedArtifact;
import com.hubspot.deploy.ExternalArtifact;
import com.hubspot.deploy.RemoteArtifact;
import com.hubspot.deploy.S3Artifact;
import com.hubspot.deploy.S3ArtifactSignature;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseConfiguration;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.s3.base.ArtifactManager;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class SingularityExecutorArtifactFetcher {

  private final LocalDownloadServiceFetcher localDownloadServiceFetcher;
  private final SingularityExecutorConfiguration executorConfiguration;
  private final SingularityS3Configuration s3Configuration;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;
  private final SingularityRunnerBaseConfiguration runnerBaseConfiguration;

  @Inject
  public SingularityExecutorArtifactFetcher(
    LocalDownloadServiceFetcher localDownloadServiceFetcher,
    SingularityS3Configuration s3Configuration,
    SingularityExecutorConfiguration executorConfiguration,
    ObjectMapper objectMapper,
    SingularityRunnerExceptionNotifier exceptionNotifier,
    SingularityRunnerBaseConfiguration runnerBaseConfiguration
  ) {
    this.localDownloadServiceFetcher = localDownloadServiceFetcher;
    this.executorConfiguration = executorConfiguration;
    this.s3Configuration = s3Configuration;
    this.exceptionNotifier = exceptionNotifier;
    this.runnerBaseConfiguration = runnerBaseConfiguration;
  }

  public SingularityExecutorTaskArtifactFetcher buildTaskFetcher(
    SingularityExecutorTask task
  ) {
    ArtifactManager artifactManager = new ArtifactManager(
      runnerBaseConfiguration,
      s3Configuration,
      task.getLog(),
      exceptionNotifier
    );

    return new SingularityExecutorTaskArtifactFetcher(artifactManager, task);
  }

  public class SingularityExecutorTaskArtifactFetcher {

    private final ArtifactManager artifactManager;
    private final SingularityExecutorTask task;

    private SingularityExecutorTaskArtifactFetcher(
      ArtifactManager artifactManager,
      SingularityExecutorTask task
    ) {
      this.artifactManager = artifactManager;
      this.task = task;
    }

    public void cancel() {
      artifactManager.markKilled();
      artifactManager.signalKillToProcessIfActive();
    }

    public void fetchFiles(
      List<EmbeddedArtifact> embeddedArtifacts,
      List<S3Artifact> s3Artifacts,
      List<S3ArtifactSignature> s3ArtifactsWithSignature,
      List<ExternalArtifact> remoteArtifacts
    ) throws InterruptedException {
      extractFiles(task, artifactManager, embeddedArtifacts);

      boolean fetchS3ArtifactsLocally = true;

      final ImmutableList<S3Artifact> allS3Artifacts = ImmutableList
        .<S3Artifact>builder()
        .addAll(s3Artifacts)
        .addAll(s3ArtifactsWithSignature)
        .build();

      if (
        executorConfiguration.isUseLocalDownloadService() && !allS3Artifacts.isEmpty()
      ) {
        final long start = System.currentTimeMillis();

        task
          .getLog()
          .info(
            "Fetching {} (S3) artifacts and {} (S3) artifact signatures from {}",
            s3Artifacts.size(),
            s3ArtifactsWithSignature.size(),
            localDownloadServiceFetcher.getDownloadPath()
          );

        try {
          localDownloadServiceFetcher.downloadFiles(allS3Artifacts, task);
          fetchS3ArtifactsLocally = false;

          task
            .getLog()
            .info(
              "Fetched {} (S3) artifacts and {} (S3) artifact signatures from local download service in {}",
              s3Artifacts.size(),
              s3ArtifactsWithSignature.size(),
              JavaUtils.duration(start)
            );
        } catch (InterruptedException ie) {
          task
            .getLog()
            .warn(
              "Interrupted while downloading S3 artifacts from local download service"
            );
          throw ie;
        } catch (Throwable t) {
          task
            .getLog()
            .error(
              "Failed downloading S3 artifacts from local download service - falling back to in-task fetch",
              t
            );
        }
      }

      if (fetchS3ArtifactsLocally) {
        for (RemoteArtifact s3Artifact : allS3Artifacts) {
          downloadRemoteArtifact(s3Artifact, artifactManager, task);
        }
      }

      for (RemoteArtifact externalArtifact : remoteArtifacts) {
        downloadRemoteArtifact(externalArtifact, artifactManager, task);
      }
    }

    private void extractFiles(
      SingularityExecutorTask task,
      ArtifactManager artifactManager,
      List<EmbeddedArtifact> embeddedArtifacts
    ) {
      for (EmbeddedArtifact artifact : embeddedArtifacts) {
        artifactManager.extract(
          artifact,
          task.getArtifactPath(artifact, task.getTaskDefinition().getTaskDirectoryPath())
        );
      }
    }

    private void downloadRemoteArtifact(
      RemoteArtifact remoteArtifact,
      ArtifactManager artifactManager,
      SingularityExecutorTask task
    ) {
      Path fetched = artifactManager.fetch(remoteArtifact);

      if (Objects.toString(fetched.getFileName()).endsWith(".tar.gz")) {
        artifactManager.untar(
          fetched,
          task.getArtifactPath(
            remoteArtifact,
            task.getTaskDefinition().getTaskDirectoryPath()
          )
        );
      } else {
        artifactManager.copy(
          fetched,
          task.getArtifactPath(
            remoteArtifact,
            task.getTaskDefinition().getTaskDirectoryPath()
          ),
          remoteArtifact.getFilename()
        );
      }
    }
  }
}
