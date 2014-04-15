package com.hubspot.singularity.executor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.deploy.ArtifactInfo;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.config.SingularityExecutorModule;

@Singleton
public class ArtifactManager {

  private final static Logger LOG = LoggerFactory.getLogger(ArtifactManager.class);

  private final Path cacheDirectory;
  
  @Inject
  public ArtifactManager(@Named(SingularityExecutorModule.ARTIFACT_CACHE_DIRECTORY) String cacheDirectory) {
    this.cacheDirectory = Paths.get(cacheDirectory);
  }
    
  private long getSize(Path path) {
    try {
      return Files.size(path);
    } catch (IOException ioe) {
      throw new RuntimeException(String.format("Couldnt get file size of %s", path), ioe);
    }
  }
  
  private boolean filesSizeMatches(ArtifactInfo artifactInfo, Path path) {
    return artifactInfo.getFilesize() < 1 || artifactInfo.getFilesize() == getSize(path);
  }
  
  private boolean md5Matches(ArtifactInfo artifactInfo, Path path) {
    return !artifactInfo.getMd5sum().isPresent() || artifactInfo.getMd5sum().get().equals(calculateMd5sum(path));
  }
  
  private void checkFilesize(ArtifactInfo artifactInfo, Path path) {
    if (!filesSizeMatches(artifactInfo, path)) {
      throw new RuntimeException(String.format("Filesize %s (%s) does not match expected (%s)", getSize(path), path, artifactInfo.getFilesize()));
    }

    if (!md5Matches(artifactInfo, path)) {
      throw new RuntimeException(String.format("Md5sum %s (%s) does not match expected (%s)", calculateMd5sum(path), path, artifactInfo.getMd5sum()));
    }
  }
  
  private Path createTempPath(String filename) {
    try {
      return Files.createTempFile(filename, null);
    } catch (IOException e) {
      throw new RuntimeException(String.format("Couldn't create temporary file for %s", filename), e);
    }
  }
  
  public void downloadAndCheck(ArtifactInfo artifactInfo, Path downloadTo) {
    downloadUri(artifactInfo.getUrl(), downloadTo);
    
    checkFilesize(artifactInfo, downloadTo);
  }
  
  private Path downloadAndCache(ArtifactInfo artifactInfo, String filename) {
    Path tempFilePath = createTempPath(filename);
    
    downloadAndCheck(artifactInfo, tempFilePath);
    
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
  
  private boolean checkCached(ArtifactInfo artifactInfo, Path cachedPath) {
    if (!Files.exists(cachedPath)) {
      LOG.debug(String.format("Cached %s did not exist", cachedPath));
      return false;
    }
    
    if (!filesSizeMatches(artifactInfo, cachedPath)) {
      LOG.debug(String.format("Cached %s (%s) did not match file size %s", cachedPath, getSize(cachedPath), artifactInfo.getFilesize()));
      return false;
    }
    
    if (!md5Matches(artifactInfo, cachedPath)) {
      LOG.debug(String.format("Cached %s (%s) did not match md5 %s", cachedPath, calculateMd5sum(cachedPath), artifactInfo.getMd5sum().get()));
      return false;
    }
    
    return true;
  }
  
  public Path fetch(ArtifactInfo artifactInfo) {
    String filename = getFilenameFromUri(artifactInfo.getUrl());
    Path cachedPath = getCachedPath(filename);
    
    if (!checkCached(artifactInfo, cachedPath)) {
      downloadAndCache(artifactInfo, filename);
    }
  
    return cachedPath;
  }
  
  private void runCommand(final List<String> command) {
    final ProcessBuilder processBuilder = new ProcessBuilder(command);

    try {
      processBuilder.inheritIO();
      
      final int exitCode = processBuilder.start().waitFor();
      
      Preconditions.checkState(exitCode == 0, "Got exit code %d while running command %s", exitCode, command);
      
    } catch (Throwable t) {
      throw new RuntimeException(String.format("While running %s", command), t);
    }
  }
  
  private String getFullPath(Path path) {
    return path.toAbsolutePath().toString();
  }
  
  private void downloadUri(String uri, Path path) {
    LOG.info(String.format("Downloading %s to %s", uri, getFullPath(path)));

    final List<String> command = Lists.newArrayList();
    command.add("wget");
    command.add(uri);
    command.add("-O");
    command.add(getFullPath(path));
    command.add("-nv");
    command.add("--no-check-certificate");

    runCommand(command);
  }

  public void untar(Path source, Path destination) {
    LOG.info(String.format("Untarring %s to %s", getFullPath(source), getFullPath(destination)));
    
    final List<String> command = Lists.newArrayList();
    command.add("tar");
    command.add("-oxzf");
    command.add(getFullPath(source));
    command.add("-C");
    command.add(getFullPath(destination));
  
    runCommand(command);
  }

  private String calculateMd5sum(Path path) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] buffer = new byte[8192];
      
      try (InputStream is = Files.newInputStream(path)) {
        DigestInputStream dis = new DigestInputStream(is, md);
        while (dis.read(buffer) != -1) {}
      }
      byte[] digest = md.digest();
      
      return JavaUtils.toString(digest);
    } catch (NoSuchAlgorithmException | IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public static String getFilenameFromUri(String uriString) {
    final URI uri = URI.create(uriString);
    final String path = uri.getPath();
    int lastIndexOf = path.lastIndexOf("/");
    if (lastIndexOf < 0) {
      return path;
    }
    return path.substring(lastIndexOf + 1);
  }

}
