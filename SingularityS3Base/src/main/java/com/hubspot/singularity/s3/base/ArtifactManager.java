package com.hubspot.singularity.s3.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.openpgp.PGPException;
import org.slf4j.Logger;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.hubspot.deploy.Artifact;
import com.hubspot.deploy.EmbeddedArtifact;
import com.hubspot.deploy.ExternalArtifact;
import com.hubspot.deploy.RemoteArtifact;
import com.hubspot.deploy.S3Artifact;
import com.hubspot.singularity.runner.base.shared.ProcessFailedException;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import com.hubspot.singularity.s3.base.gpg.DetachedSignatureVerifier;

public class ArtifactManager extends SimpleProcessManager {

  private final Path cacheDirectory;
  private final Logger log;
  private final S3ArtifactDownloader s3ArtifactDownloader;
  private final SingularityS3Configuration configuration;

  public ArtifactManager(SingularityS3Configuration configuration, Logger log) {
    super(log);

    this.cacheDirectory = Paths.get(configuration.getArtifactCacheDirectory());
    this.log = log;
    this.s3ArtifactDownloader = new S3ArtifactDownloader(configuration, log);
    this.configuration = configuration;
  }

  private long getSize(Path path) {
    try {
      return Files.size(path);
    } catch (IOException ioe) {
      throw new RuntimeException(String.format("Couldnt get file size of %s", path), ioe);
    }
  }

  private boolean filesSizeMatches(RemoteArtifact artifact, Path path) {
    return !artifact.getFilesize().isPresent() || (artifact.getFilesize().get() == getSize(path));
  }

  private boolean md5Matches(Artifact artifact, Path path) {
    return !artifact.getMd5sum().isPresent() || artifact.getMd5sum().get().equals(calculateMd5sum(path));
  }

  private void checkFilesize(RemoteArtifact artifact, Path path) {
    if (!filesSizeMatches(artifact, path)) {
      throw new RuntimeException(String.format("Filesize %s (%s) does not match expected (%s)", getSize(path), path, artifact.getFilesize()));
    }
  }

  private void checkMd5(Artifact artifact, Path path) {
    if (!md5Matches(artifact, path)) {
      throw new RuntimeException(String.format("Md5sum %s (%s) does not match expected (%s)", calculateMd5sum(path), path, artifact.getMd5sum().get()));
    }
  }

  private Path createTempPath(String filename) {
    try {
      return Files.createTempFile(cacheDirectory, filename, null);
    } catch (IOException e) {
      throw new RuntimeException(String.format("Couldn't create temporary file for %s", filename), e);
    }
  }

  private void downloadAndCheck(RemoteArtifact artifact, Path downloadTo) {
    if (artifact instanceof ExternalArtifact) {
      downloadExternalArtifact((ExternalArtifact) artifact, downloadTo);
    } else if (artifact instanceof S3Artifact) {
      downloadS3Artifact((S3Artifact) artifact, downloadTo);
    } else {
      throw new IllegalArgumentException("Unknown artifact type: " + artifact.getClass());
    }

    checkFilesize(artifact, downloadTo);
    checkMd5(artifact, downloadTo);
    checkGpgSignature(artifact, downloadTo);
  }

  private void checkGpgSignature(RemoteArtifact baseArtifact, Path downloadTo) {
    if (baseArtifact.getGpgSignatureArtifact().isPresent() && baseArtifact.getGpgSignatureArtifact().get() instanceof RemoteArtifact) {

      Path signatureArtifactPath =  fetch((RemoteArtifact) baseArtifact.getGpgSignatureArtifact().get());

      File gpgBin = new File(configuration.getGpgBinaryPath());
      if (!gpgBin.exists() || !gpgBin.canExecute()) {
        throw new RuntimeException(String.format("gpg binary at %s not found or not executable", configuration.getGpgBinaryPath()));
      }

      String verifyCmd = String.format("%s --batch --yes --passphrase-fd 0 --homedir %s -u %s --verify %s", configuration.getGpgBinaryPath(), configuration.getGpgHome(), configuration.getGpgKeyUsername(), signatureArtifactPath);
      try {
        Process p = Runtime.getRuntime().exec(verifyCmd);
        OutputStream stdin = p.getOutputStream();
        stdin.write(configuration.getGpgKeyPassword().getBytes());
        p.wait(TimeUnit.SECONDS.toMillis(5));
        int returnCode = p.exitValue();
        if (returnCode != 0) {
          throw new RuntimeException(String.format("Gpg verify failed (rc: %s) could not verify signature %s for file %s", Integer.toString(returnCode), signatureArtifactPath.toString(), downloadTo.toString()));
        } else {
          return;
        }
      } catch (IOException e) {
        throw Throwables.propagate(e);
      } catch (InterruptedException e) {
        throw Throwables.propagate(e);
      }
    }
  }

  public void extract(EmbeddedArtifact embeddedArtifact, Path directory) {
    final Path extractTo = directory.resolve(embeddedArtifact.getFilename());

    final Path parent = extractTo.getParent();
    try {
      if (parent != null) {
        Files.createDirectories(parent);
      }
    } catch (IOException e) {
      throw new RuntimeException(String.format("Couldn't extract %s, unable to create directory %s", embeddedArtifact.getName(), parent), e);
    }

    log.info("Extracting {} bytes of {} to {}", embeddedArtifact.getContent().length, embeddedArtifact.getName(), extractTo);

    try (SeekableByteChannel byteChannel = Files.newByteChannel(extractTo, EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
      byteChannel.write(ByteBuffer.wrap(embeddedArtifact.getContent()));
    } catch (IOException e) {
      throw new RuntimeException(String.format("Couldn't extract %s", embeddedArtifact.getName()), e);
    }

    checkMd5(embeddedArtifact, extractTo);
  }

  private Path downloadAndCache(RemoteArtifact artifact, String filename) {
    Path tempFilePath = createTempPath(filename);

    downloadAndCheck(artifact, tempFilePath);

    Path cachedPath = getCachedPath(filename);

    try {
      Files.move(tempFilePath, cachedPath, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new RuntimeException(String.format("Couldn't move %s to %s", tempFilePath, cachedPath), e);
    }

    return cachedPath;
  }

  private Path getCachedPath(String filename) {
    return cacheDirectory.resolve(filename);
  }

  private boolean checkCached(RemoteArtifact artifact, Path cachedPath) {
    if (!Files.exists(cachedPath)) {
      log.debug("Cached {} did not exist", cachedPath);
      return false;
    }

    if (!filesSizeMatches(artifact, cachedPath)) {
      log.debug("Cached {} ({}) did not match file size {}", cachedPath, getSize(cachedPath), artifact.getFilesize());
      return false;
    }

    if (!md5Matches(artifact, cachedPath)) {
      log.debug("Cached {} ({}) did not match md5 {}", cachedPath, calculateMd5sum(cachedPath), artifact.getMd5sum().get());
      return false;
    }

    return true;
  }

  public Path fetch(RemoteArtifact artifact) {
    String filename = artifact.getFilename();
    Path cachedPath = getCachedPath(filename);

    if (!checkCached(artifact, cachedPath)) {
      downloadAndCache(artifact, filename);
    } else {
      log.info("Using cached file {}", cachedPath);
    }

    return cachedPath;
  }

  private void downloadExternalArtifact(ExternalArtifact externalArtifact, Path downloadTo) {
    downloadUri(externalArtifact.getUrl(), downloadTo);
  }

  private void downloadS3Artifact(S3Artifact s3Artifact, Path downloadTo) {
    s3ArtifactDownloader.download(s3Artifact, downloadTo);
  }

  private void downloadUri(String uri, Path path) {
    log.info("Downloading {} to {}", uri, path);

    final List<String> command = ImmutableList.of(
        "wget",
        uri,
        "-O",
        path.toString(),
        "-nv",
        "--no-check-certificate");

    runCommandAndThrowRuntimeException(command);
  }

  public void copy(Path source, Path destination) {
    log.info("Copying {} to {}", source, destination);

    try {
      Files.copy(source, destination.resolve(source.getFileName()));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public void untar(Path source, Path destination) {
    log.info("Untarring {} to {}", source, destination);

    final List<String> command = ImmutableList.of(
        "tar",
        "-oxzf",
        source.toString(),
        "-C",
        destination.toString());

    runCommandAndThrowRuntimeException(command);
  }

  private void runCommandAndThrowRuntimeException(List<String> command) {
    try {
      super.runCommand(command);
    } catch (InterruptedException | ProcessFailedException e) {
      throw Throwables.propagate(e);
    }
  }

  private String calculateMd5sum(Path path) {
    try {
      HashCode hc = com.google.common.io.Files.hash(path.toFile(), Hashing.md5());

      return hc.toString();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

}
