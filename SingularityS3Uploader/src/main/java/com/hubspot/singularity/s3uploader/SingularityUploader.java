package com.hubspot.singularity.s3uploader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer.Context;
import com.github.rholder.retry.RetryException;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderConfiguration;

public abstract class SingularityUploader {
  static final Logger LOG = LoggerFactory.getLogger(SingularityUploader.class);
  private static final String LOG_START_TIME_ATTR = "logstart";
  private static final String LOG_END_TIME_ATTR = "logend";

  final S3UploadMetadata uploadMetadata;
  private final PathMatcher pathMatcher;
  private final Optional<PathMatcher> finishedPathMatcher;
  final String fileDirectory;
  final String bucketName;
  final Path metadataPath;
  private final SingularityS3UploaderMetrics metrics;
  final String logIdentifier;
  final String hostname;
  final SingularityS3UploaderConfiguration configuration;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;

  SingularityUploader(S3UploadMetadata uploadMetadata, FileSystem fileSystem, SingularityS3UploaderMetrics metrics, Path metadataPath,
                      SingularityS3UploaderConfiguration configuration, String hostname, SingularityRunnerExceptionNotifier exceptionNotifier) {
    this.metrics = metrics;
    this.uploadMetadata = uploadMetadata;
    this.fileDirectory = uploadMetadata.getDirectory();
    this.pathMatcher = fileSystem.getPathMatcher("glob:" + uploadMetadata.getFileGlob());

    if (uploadMetadata.getOnFinishGlob().isPresent()) {
      finishedPathMatcher = Optional.of(fileSystem.getPathMatcher("glob:" + uploadMetadata.getOnFinishGlob().get()));
    } else {
      finishedPathMatcher = Optional.<PathMatcher> absent();
    }

    this.hostname = hostname;
    this.bucketName = uploadMetadata.getS3Bucket();
    this.metadataPath = metadataPath;
    this.logIdentifier = String.format("[%s]", metadataPath.getFileName());
    this.configuration = configuration;
    this.exceptionNotifier = exceptionNotifier;
  }

  protected abstract void uploadSingle(int sequence, Path file) throws Exception;

  private void uploadBatch(List<Path> toUpload) {
    final long start = System.currentTimeMillis();
    LOG.info("{} Uploading {} item(s)", logIdentifier, toUpload.size());

    int success = 0;

    for (int i = 0; i < toUpload.size(); i++) {
      final Context context = metrics.getUploadTimer().time();
      final Path file = toUpload.get(i);
      if (!configuration.isCheckForOpenFiles() || !isFileOpen(file)) {
        try {
          uploadSingle(i, file);
          metrics.upload();
          success++;
          Files.delete(file);
        } catch (RetryException re) {
          metrics.error();
          LOG.warn("{} Couldn't upload or delete {}", logIdentifier, file, re);
          exceptionNotifier.notify(String.format("%s exception during upload", re.getCause().getClass()), re.getCause(), ImmutableMap.of("logIdentifier", logIdentifier, "file", file.toString(), "failedAttempts", Integer.toString(re.getNumberOfFailedAttempts())));
        } catch (Exception e) {
          metrics.error();
          LOG.warn("{} Couldn't upload or delete {}", logIdentifier, file, e);
          exceptionNotifier.notify(String.format("Error during upload (%s)", e.getMessage()), e, ImmutableMap.of("logIdentifier", logIdentifier, "file", file.toString()));
        } finally {
          context.stop();
        }
      } else {
        LOG.info("{} is in use by another process, will retry upload later", file);
      }
    }

    LOG.info("{} Uploaded {} out of {} item(s) in {}", logIdentifier, success, toUpload.size(), JavaUtils.duration(start));
  }

  Path getMetadataPath() {
    return metadataPath;
  }

  S3UploadMetadata getUploadMetadata() {
    return uploadMetadata;
  }

  int upload(Set<Path> synchronizedToUpload, boolean isFinished) throws IOException {
    final List<Path> toUpload = Lists.newArrayList();
    int found = 0;

    final Path directory = Paths.get(fileDirectory);

    if (!Files.exists(directory)) {
      LOG.info("Path {} doesn't exist", fileDirectory);
      return found;
    }

    for (Path file : JavaUtils.iterable(directory)) {
      found += handleFile(file, isFinished, synchronizedToUpload, toUpload);
    }

    if (toUpload.isEmpty()) {
      return found;
    }

    uploadBatch(toUpload);

    return found;
  }

  int handleFile(Path path, boolean isFinished, Set<Path> synchronizedToUpload, List<Path> toUpload) throws IOException {
    int found = 0;
    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
      if (uploadMetadata.isCheckSubdirectories()) {
        LOG.debug("{} was a directory, checking files in directory", path);
        for (Path file : JavaUtils.iterable(path)) {
          found += handleFile(file, isFinished, synchronizedToUpload, toUpload);
        }
      } else {
        LOG.debug("{} was a directory, skipping", path);
      }
      return found;
    }

    if (!pathMatcher.matches(path.getFileName())) {
      if (!isFinished || !finishedPathMatcher.isPresent() || !finishedPathMatcher.get().matches(path.getFileName())) {
        LOG.trace("{} Skipping {} because it doesn't match {}", logIdentifier, path, uploadMetadata.getFileGlob());
        return found;
      } else {
        LOG.trace("Not skipping file {} because it matched finish glob {}", path, uploadMetadata.getOnFinishGlob().get());
      }
    }

    if (Files.size(path) == 0) {
      LOG.trace("{} Skipping {} because its size is 0", logIdentifier, path);
      return found;
    }

    found++;

    if (synchronizedToUpload.add(path)) {
      toUpload.add(path);
    } else {
      LOG.debug("{} Another uploader already added {}", logIdentifier, path);
    }
    return found;
  }

  static boolean isFileOpen(Path path) {
    try {
      SimpleProcessManager lsof = new SimpleProcessManager(LOG);
      List<String> cmd = ImmutableList.of("lsof", path.toAbsolutePath().toString());
      List<String> output = lsof.runCommandWithOutput(cmd, Sets.newHashSet(0, 1));
      for (String line : output) {
        if (line.contains(path.toAbsolutePath().toString())) {
          return true;
        }
      }
    } catch (Exception e) {
      LOG.error("Could not determine if file {} was in use, skipping", path, e);
      return true;
    }
    return false;
  }

  Optional<Long> readFileAttributeAsLong(Path file, String attribute, UserDefinedFileAttributeView view, List<String> knownAttributes) {
    if (knownAttributes.contains(attribute)) {
      try {
        LOG.trace("Attempting to read attribute {}, from file {}", attribute, file);
        ByteBuffer buf = ByteBuffer.allocate(view.size(attribute));
        view.read(attribute, buf);
        buf.flip();
        String value = Charset.defaultCharset().decode(buf).toString();
        if (Strings.isNullOrEmpty(value)) {
          LOG.debug("No attrbiute {} found for file {}", attribute, file);
          return Optional.absent();
        }
        return Optional.of(Long.parseLong(value));
      } catch (Exception e) {
        LOG.error("Error getting extra file metadata for {}", file, e);
        return Optional.absent();
      }
    } else {
      return Optional.absent();
    }
  }

  boolean shouldApplyStorageClass(long fileSizeBytes, Optional<String> maybeStorageClass) {
    if (!maybeStorageClass.isPresent()) {
      return false;
    }

    if (!getUploadMetadata().getApplyStorageClassIfOverBytes().isPresent()) {
      return true;
    }

    return fileSizeBytes > getUploadMetadata().getApplyStorageClassIfOverBytes().get();
  }

  UploaderFileAttributes getFileAttributes(Path file) {
    Set<String> supportedViews = FileSystems.getDefault().supportedFileAttributeViews();
    LOG.trace("Supported attribute views are {}", supportedViews);
    if (supportedViews.contains("user")) {
      try {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
        List<String> attributes = view.list();
        LOG.debug("Found file attributes {} for file {}", attributes, file);
        Optional<Long> maybeStartTime = readFileAttributeAsLong(file, LOG_START_TIME_ATTR, view, attributes);
        Optional<Long> maybeEndTime = readFileAttributeAsLong(file, LOG_END_TIME_ATTR, view, attributes);
        return new UploaderFileAttributes(maybeStartTime, maybeEndTime);
      } catch (Exception e) {
        LOG.error("Could not get extra file metadata for {}", file, e);
      }
    }
    return new UploaderFileAttributes(Optional.absent(), Optional.absent());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SingularityS3Uploader that = (SingularityS3Uploader) o;

    if (uploadMetadata != null ? !uploadMetadata.equals(that.uploadMetadata) : that.uploadMetadata != null) {
      return false;
    }
    if (fileDirectory != null ? !fileDirectory.equals(that.fileDirectory) : that.fileDirectory != null) {
      return false;
    }
    if (bucketName != null ? !bucketName.equals(that.bucketName) : that.bucketName != null) {
      return false;
    }
    if (metadataPath != null ? !metadataPath.equals(that.metadataPath) : that.metadataPath != null) {
      return false;
    }
    if (logIdentifier != null ? !logIdentifier.equals(that.logIdentifier) : that.logIdentifier != null) {
      return false;
    }
    return hostname != null ? hostname.equals(that.hostname) : that.hostname == null;
  }

  @Override
  public int hashCode() {
    int result = uploadMetadata != null ? uploadMetadata.hashCode() : 0;
    result = 31 * result + (fileDirectory != null ? fileDirectory.hashCode() : 0);
    result = 31 * result + (bucketName != null ? bucketName.hashCode() : 0);
    result = 31 * result + (metadataPath != null ? metadataPath.hashCode() : 0);
    result = 31 * result + (logIdentifier != null ? logIdentifier.hashCode() : 0);
    result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
    return result;
  }
}
