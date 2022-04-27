package com.hubspot.singularity.executor.task;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.hubspot.deploy.S3Artifact;
import com.hubspot.deploy.S3ArtifactSignature;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;

public class SingularityExecutorArtifactVerifier {

  private final Logger log;
  private final SingularityExecutorConfiguration executorConfiguration;
  private final SingularityExecutorTaskDefinition taskDefinition;
  private final SingularityS3Configuration s3Configuration;

  public SingularityExecutorArtifactVerifier(
    SingularityExecutorTaskDefinition taskDefinition,
    Logger log,
    SingularityExecutorConfiguration executorConfiguration,
    SingularityS3Configuration s3Configuration
  ) {
    this.log = log;
    this.executorConfiguration = executorConfiguration;
    this.taskDefinition = taskDefinition;
    this.s3Configuration = s3Configuration;
  }

  public void checkSignatures(
    List<S3Artifact> s3Artifacts,
    List<S3ArtifactSignature> s3ArtifactsWithSignatures
  ) {
    Map<String, S3Artifact> artifactsByFilename = s3Artifacts
      .stream()
      .collect(
        Collectors.toMap(
          S3Artifact::getFilename,
          Function.identity(),
          (lhsDuplicate, rhsDuplicate) -> {
            log.warn(
              "Duplicate artifact filenames found ({}; {})",
              lhsDuplicate,
              rhsDuplicate
            );
            return lhsDuplicate;
          }
        )
      );
    Map<String, S3ArtifactSignature> signaturesByFilename = s3ArtifactsWithSignatures
      .stream()
      .collect(
        Collectors.toMap(
          S3ArtifactSignature::getArtifactFilename,
          Function.identity(),
          (lhsDuplicate, rhsDuplicate) -> {
            log.warn(
              "Duplicate signature filenames found ({}; {})",
              lhsDuplicate,
              rhsDuplicate
            );
            return lhsDuplicate;
          }
        )
      );

    SetView<String> signaturesMissingArtifacts = Sets.difference(
      signaturesByFilename.keySet(),
      artifactsByFilename.keySet()
    );

    if (signaturesMissingArtifacts.size() > 0) {
      if (executorConfiguration.isFailOnSignatureWithNoMatchingArtifact()) {
        throw new ArtifactVerificationException(
          String.format(
            "No matching artifact(s) found for signature(s) %s",
            signaturesMissingArtifacts
          )
        );
      } else {
        log.warn(
          "No matching artifact(s) found for signature(s) {}",
          signaturesMissingArtifacts
        );
      }
    }

    SetView<String> artifactsMissingSignatures = Sets.difference(
      artifactsByFilename.keySet(),
      signaturesByFilename.keySet()
    );
    if (artifactsMissingSignatures.size() > 0) {
      if (executorConfiguration.isFailOnArtifactWithNoMatchingSignature()) {
        throw new ArtifactVerificationException(
          String.format(
            "No signature(s) found for artifact(s) %s",
            artifactsMissingSignatures
          )
        );
      } else {
        log.warn("No signature(s) found for artifact(s) {}", artifactsMissingSignatures);
      }
    }

    if (s3ArtifactsWithSignatures.isEmpty()) {
      log.info(
        "No files containing artifact signatures specified, skipping verification."
      );
      return;
    }

    for (S3ArtifactSignature s3ArtifactSignature : s3ArtifactsWithSignatures) {
      S3Artifact maybeMatchingForSignature = artifactsByFilename.get(
        s3ArtifactSignature.getArtifactFilename()
      );
      if (maybeMatchingForSignature != null) {
        checkArtifactSignature(maybeMatchingForSignature, s3ArtifactSignature);
      }
    }
  }

  private void checkArtifactSignature(
    S3Artifact s3Artifact,
    S3ArtifactSignature s3ArtifactSignature
  ) {
    final Path artifactPath = Paths.get(
      s3Configuration.getArtifactCacheDirectory(),
      s3Artifact.getFilenameForCache()
    );
    final Path artifactSignaturePath = Paths.get(
      s3Configuration.getArtifactCacheDirectory(),
      s3ArtifactSignature.getFilenameForCache()
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
          throw new ArtifactVerificationException(
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
