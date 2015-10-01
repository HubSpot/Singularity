package com.hubspot.singularity.executor.task;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.base.Charsets;
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
    if (taskDefinition.getExecutorData().getS3ArtifactSignatures().isEmpty()) {
      log.info("No s3 artifact signatures, skipping verification.");
      return;
    }

    for (S3ArtifactSignature s3ArtifactSignature : taskDefinition.getExecutorData().getS3ArtifactSignatures()) {
      checkArtifactSignature(s3ArtifactSignature);
    }
  }

  private void checkArtifactSignature(S3ArtifactSignature s3ArtifactSignature) {
    final Path artifactPath = Paths.get(s3Configuration.getArtifactCacheDirectory(), s3ArtifactSignature.getArtifactFilename());

    if (!Files.exists(artifactPath)) {
      log.warn("Artifact {} not found for signature {}", artifactPath, s3ArtifactSignature);
      return;
    }

    final List<String> verifyCommand = new ArrayList<>(executorConfiguration.getArtifactSignatureVerificationCommand().size());

    for (String arg : executorConfiguration.getArtifactSignatureVerificationCommand()) {
      verifyCommand.add(arg.replace("{artifactPath}", artifactPath.toString()));
    }

    try {
      final ProcessBuilder processBuilder = new ProcessBuilder(verifyCommand);

      processBuilder.directory(taskDefinition.getTaskDirectoryPath().toFile());

      processBuilder.redirectError(taskDefinition.getSignatureVerifyOutPath().toFile());
      processBuilder.redirectOutput(taskDefinition.getSignatureVerifyOutPath().toFile());

      final Process p = processBuilder.start();

      try {
        if (executorConfiguration.getArtifactSignaturePassphrase().isPresent()) {
          final PrintStream passphrasePrintStream = new PrintStream(p.getOutputStream(), true, Charsets.UTF_8.toString());
          passphrasePrintStream.println(executorConfiguration.getArtifactSignaturePassphrase().get());
          passphrasePrintStream.close();
        }

        p.wait(executorConfiguration.getArtifactSignatureVerificationCommandTimeoutMs());

        if (p.exitValue() != 0) {
          log.error("Failed to validate signature {} for artifact {}", s3ArtifactSignature.getFilename(), s3ArtifactSignature.getArtifactFilename());

          if (executorConfiguration.isFailTaskOnInvalidArtifactSignature()) {
            throw new RuntimeException(String.format("Failed to validate signature for artifact %s", artifactPath));
          }
        }
      } catch (IllegalThreadStateException itse) {
        log.warn("Signature validation command {} for artifact {} didn't complete after {}ms. Killing process.", verifyCommand, s3ArtifactSignature.getArtifactFilename(), executorConfiguration.getArtifactSignatureVerificationCommandTimeoutMs());

        p.destroy();

        if (executorConfiguration.isFailTaskOnInvalidArtifactSignature()) {
          throw new RuntimeException(String.format("Signature validation command '%s' for artifact %s didn't complete after %sms.", verifyCommand, s3ArtifactSignature.getArtifactFilename(), executorConfiguration.getArtifactSignatureVerificationCommandTimeoutMs()));
        }
      }
    } catch (InterruptedException | IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
