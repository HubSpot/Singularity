package com.hubspot.singularity.executor.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.base.Throwables;
import com.hubspot.deploy.S3ArtifactSignature;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;

public class SingularityExecutorArtifactVerifier {
  private final Logger log;
  private final SingularityExecutorConfiguration executorConfiguration;
  private final SingularityS3Configuration s3Configuration;
  private final SingularityExecutorTaskDefinition taskDefinition;

  public SingularityExecutorArtifactVerifier(SingularityExecutorTaskDefinition taskDefinition, Logger log, SingularityExecutorConfiguration executorConfiguration, SingularityS3Configuration s3Configuration) {
    this.log = log;
    this.executorConfiguration = executorConfiguration;
    this.s3Configuration = s3Configuration;
    this.taskDefinition = taskDefinition;
  }

  public void checkSignatures() {
    if (!taskDefinition.getExecutorData().getS3ArtifactSignatures().isPresent() || taskDefinition.getExecutorData().getS3ArtifactSignatures().get().isEmpty()) {
      log.info("No files containing artifact signatures specified, skipping verification.");
      return;
    }

    for (S3ArtifactSignature s3ArtifactSignature : taskDefinition.getExecutorData().getS3ArtifactSignatures().get()) {
      checkArtifactSignature(s3ArtifactSignature);
    }
  }

  private void checkArtifactSignature(S3ArtifactSignature s3ArtifactSignature) {
    final Path artifactPath = Paths.get(s3Configuration.getArtifactCacheDirectory(), s3ArtifactSignature.getArtifactFilename());
    final Path artifactSignaturePath = Paths.get(s3Configuration.getArtifactCacheDirectory(), s3ArtifactSignature.getFilename());

    if (!Files.exists(artifactPath)) {
      log.warn("Artifact {} not found for signature {}", artifactPath, s3ArtifactSignature);
      return;
    }

    final List<String> verifyCommand = new ArrayList<>(executorConfiguration.getArtifactSignatureVerificationCommand().size());

    for (String arg : executorConfiguration.getArtifactSignatureVerificationCommand()) {
      verifyCommand.add(arg.replace("{artifactPath}", artifactPath.toString()).replace("{artifactSignaturePath}", artifactSignaturePath.toString()));
    }

    try {
      final ProcessBuilder processBuilder = new ProcessBuilder(verifyCommand);

      processBuilder.directory(taskDefinition.getTaskDirectoryPath().toFile());

      processBuilder.redirectError(taskDefinition.getSignatureVerifyOutPath().toFile());
      processBuilder.redirectOutput(taskDefinition.getSignatureVerifyOutPath().toFile());

      final Process p = processBuilder.start();

      p.waitFor();  // TODO: add some sort of timeout?

      if (p.exitValue() != 0) {
        log.error("Failed to validate signature in file {} for artifact file {}", s3ArtifactSignature.getFilename(), s3ArtifactSignature.getArtifactFilename());

        if (executorConfiguration.isFailTaskOnInvalidArtifactSignature()) {
          throw new RuntimeException(String.format("Failed to validate signature for artifact %s", artifactPath));
        }
      } else {
        log.info("Signature in {} for artifact {} is valid!", s3ArtifactSignature.getFilename(), s3ArtifactSignature.getArtifactFilename());
      }
    } catch (InterruptedException | IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
