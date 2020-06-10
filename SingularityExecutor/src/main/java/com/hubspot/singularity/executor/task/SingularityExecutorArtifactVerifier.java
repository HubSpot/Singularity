package com.hubspot.singularity.executor.task;

import com.hubspot.deploy.S3Artifact;
import com.hubspot.deploy.S3ArtifactSignature;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;

public class SingularityExecutorArtifactVerifier {
  private final Logger log;
  private final SingularityExecutorConfiguration executorConfiguration;
  private final SingularityExecutorTaskDefinition taskDefinition;

  public SingularityExecutorArtifactVerifier(
    SingularityExecutorTaskDefinition taskDefinition,
    Logger log,
    SingularityExecutorConfiguration executorConfiguration
  ) {
    this.log = log;
    this.executorConfiguration = executorConfiguration;
    this.taskDefinition = taskDefinition;
  }

  public void checkSignatures(
    SingularityExecutorTask task,
    List<S3Artifact> s3Artifacts,
    List<S3ArtifactSignature> s3ArtifactsWithSignatures
  ) {
    if (s3ArtifactsWithSignatures.isEmpty()) {
      log.info(
        "No files containing artifact signatures specified, skipping verification."
      );
      return;
    }

    for (S3ArtifactSignature s3ArtifactSignature : s3ArtifactsWithSignatures) {
      Optional<S3Artifact> maybeMatchingForSignature = s3Artifacts
        .stream()
        .filter(s -> s3ArtifactSignature.getArtifactFilename().equals(s.getFilename()))
        .findFirst();
      if (maybeMatchingForSignature.isPresent()) {
        checkArtifactSignature(
          task,
          maybeMatchingForSignature.get(),
          s3ArtifactSignature
        );
      } else {
        log.warn("No matching artifact found for signature {}", s3ArtifactSignature);
      }
    }
  }

  private void checkArtifactSignature(
    SingularityExecutorTask task,
    S3Artifact s3Artifact,
    S3ArtifactSignature s3ArtifactSignature
  ) {
    final Path artifactPath = task.getArtifactPath(
      s3Artifact,
      task.getTaskDefinition().getTaskDirectoryPath()
    );
    final Path artifactSignaturePath = task.getArtifactPath(
      s3ArtifactSignature,
      task.getTaskDefinition().getTaskDirectoryPath()
    );

    if (!Files.exists(artifactPath)) {
      log.warn(
        "Artifact {} not found for signature {}",
        artifactPath,
        s3ArtifactSignature
      );
      return;
    }

    final List<String> verifyCommand = new ArrayList<>(
      executorConfiguration.getArtifactSignatureVerificationCommand().size()
    );

    for (String arg : executorConfiguration.getArtifactSignatureVerificationCommand()) {
      verifyCommand.add(
        arg
          .replace("{artifactPath}", artifactPath.toString())
          .replace("{artifactSignaturePath}", artifactSignaturePath.toString())
      );
    }

    try {
      final ProcessBuilder processBuilder = new ProcessBuilder(verifyCommand);

      processBuilder.directory(taskDefinition.getTaskDirectoryPath().toFile());

      processBuilder.redirectError(taskDefinition.getSignatureVerifyOutPath().toFile());
      processBuilder.redirectOutput(taskDefinition.getSignatureVerifyOutPath().toFile());

      final Process p = processBuilder.start();

      p.waitFor(); // TODO: add some sort of timeout?

      if (p.exitValue() != 0) {
        log.error(
          "Failed to validate signature in file {} for artifact file {}",
          s3ArtifactSignature.getFilename(),
          s3ArtifactSignature.getArtifactFilename()
        );

        if (executorConfiguration.isFailTaskOnInvalidArtifactSignature()) {
          throw new RuntimeException(
            String.format("Failed to validate signature for artifact %s", artifactPath)
          );
        }
      } else {
        log.info(
          "Signature in {} for artifact {} is valid!",
          s3ArtifactSignature.getFilename(),
          s3ArtifactSignature.getArtifactFilename()
        );
      }
    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
