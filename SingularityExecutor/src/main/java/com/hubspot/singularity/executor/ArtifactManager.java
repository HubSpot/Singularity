package com.hubspot.singularity.executor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.hubspot.deploy.Artifact;
import com.hubspot.deploy.EmbeddedArtifact;
import com.hubspot.deploy.ExternalArtifact;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;

public class ArtifactManager extends SimpleProcessManager {

  private final Path cacheDirectory;
  private final Logger log;
  private final String taskId;
  
  public ArtifactManager(SingularityExecutorConfiguration configuration, String taskId, Logger log) {
    super(log, configuration.getExecutorBashLogPath(taskId));
    
    this.cacheDirectory = Paths.get(configuration.getCacheDirectory());
    this.log = log;
    this.taskId = taskId;
  }

  private long getSize(Path path) {
    try {
      return Files.size(path);
    } catch (IOException ioe) {
      throw new RuntimeException(String.format("Couldnt get file size of %s", path), ioe);
    }
  }
  
  private boolean filesSizeMatches(ExternalArtifact artifact, Path path) {
    return !artifact.getFilesize().isPresent() || artifact.getFilesize().get() == getSize(path);
  }
  
  private boolean md5Matches(Artifact artifact, Path path) {
    return !artifact.getMd5sum().isPresent() || artifact.getMd5sum().get().equals(calculateMd5sum(path));
  }
  
  private void checkFilesize(ExternalArtifact artifact, Path path) {
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
  
  private void downloadAndCheck(ExternalArtifact artifact, Path downloadTo) {
    downloadUri(artifact.getUrl(), downloadTo);
    
    checkFilesize(artifact, downloadTo);
    checkMd5(artifact, downloadTo);
  }
  
  public void extract(EmbeddedArtifact embeddedArtifact, Path directory) {
    final Path extractTo = directory.resolve(embeddedArtifact.getFilename());
    
    log.info("Extracting {} to {}", embeddedArtifact.getName(), extractTo);
    
    try (SeekableByteChannel byteChannel = Files.newByteChannel(directory.resolve(embeddedArtifact.getFilename()), EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
      byteChannel.write(ByteBuffer.wrap(embeddedArtifact.getContent()));
    } catch (IOException e) {
      throw new RuntimeException(String.format("Couldn't extract %s", embeddedArtifact.getName()), e);
    }
    
    checkMd5(embeddedArtifact, extractTo);
  }
  
  private Path downloadAndCache(ExternalArtifact artifact, String filename) {
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
  
  private boolean checkCached(ExternalArtifact artifact, Path cachedPath) {
    if (!Files.exists(cachedPath)) {
      log.debug("Cached {} did not exist", taskId, cachedPath);
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
  
  public Path fetch(ExternalArtifact artifact) {
    String filename = artifact.getFilename();
    Path cachedPath = getCachedPath(filename);
    
    if (!checkCached(artifact, cachedPath)) {
      downloadAndCache(artifact, filename);
    } else {
      log.info("Using cached file {}", cachedPath);
    }
  
    return cachedPath;
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

    runCommand(command);
  }

  public void untar(Path source, Path destination) {
    log.info("Untarring {} to {}", source, destination);
    
    final List<String> command = ImmutableList.of(
      "tar",
      "-oxzf",
      source.toString(),
      "-C",
      destination.toString());
  
    runCommand(command);
  }

  private String calculateMd5sum(Path path) {
    try {
      HashCode hc = com.google.common.io.Files.hash(path.toFile(), Hashing.md5());
      
      return hc.toString();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String toString() {
    return "ArtifactManager [cacheDirectory=" + cacheDirectory + ", log=" + log + ", taskId=" + taskId + "]";
  }

}
