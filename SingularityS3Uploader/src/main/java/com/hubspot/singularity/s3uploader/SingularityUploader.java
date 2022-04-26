package com.hubspot.singularity.s3uploader;

import com.codahale.metrics.Timer.Context;
import com.github.rholder.retry.RetryException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.runner.base.shared.CompressionType;
import com.hubspot.singularity.runner.base.shared.ProcessFailedException;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SingularityUploader {

  static final Logger LOG = LoggerFactory.getLogger(SingularityUploader.class);
  private static final String LOG_START_TIME_ATTR = "logstart";
  private static final String LOG_END_TIME_ATTR = "logend";
  private static final long CHECK_FILE_OPEN_TIMEOUT_MILLIS = 1500;

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
  private final Lock checkFileOpenLock;

  SingularityUploader(
    S3UploadMetadata uploadMetadata,
    FileSystem fileSystem,
    SingularityS3UploaderMetrics metrics,
    Path metadataPath,
    SingularityS3UploaderConfiguration configuration,
    String hostname,
    SingularityRunnerExceptionNotifier exceptionNotifier,
    Lock checkFileOpenLock
  ) {
    this.metrics = metrics;
    this.uploadMetadata = uploadMetadata;
    this.fileDirectory = uploadMetadata.getDirectory();
    this.pathMatcher = fileSystem.getPathMatcher("glob:" + uploadMetadata.getFileGlob());

    if (uploadMetadata.getOnFinishGlob().isPresent()) {
      finishedPathMatcher =
        Optional.of(
          fileSystem.getPathMatcher("glob:" + uploadMetadata.getOnFinishGlob().get())
        );
    } else {
      finishedPathMatcher = Optional.empty();
    }

    this.hostname = hostname;
    this.bucketName = uploadMetadata.getS3Bucket();
    this.metadataPath = metadataPath;
    this.logIdentifier = String.format("[%s]", metadataPath.getFileName());
    this.configuration = configuration;
    this.exceptionNotifier = exceptionNotifier;

    this.checkFileOpenLock = checkFileOpenLock;
  }

  protected abstract void uploadSingle(int sequence, Path file) throws Exception;

  int uploadBatch(List<Path> toUpload) {
    final long start = System.currentTimeMillis();
    LOG.info("{} Uploading {} item(s)", logIdentifier, toUpload.size());

    int success = 0;

    for (int i = 0; i < toUpload.size(); i++) {
      final Context context = metrics.getUploadTimer().time();
      final Path file = toUpload.get(i);
      if (
        !configuration.isCheckForOpenFiles() ||
        !uploadMetadata.isCheckIfOpen() ||
        uploadMetadata.isImmediate() ||
        !isFileOpen(file, configuration.isCheckOpenFilesViaFuser())
      ) {
        try {
          uploadSingle(i, file);
          metrics.upload();
          success++;
          Files.delete(file);
        } catch (RetryException re) {
          metrics.error();
          LOG.warn("{} Couldn't upload or delete {}", logIdentifier, file, re);
          exceptionNotifier.notify(
            String.format("%s exception during upload", re.getCause().getClass()),
            re.getCause(),
            ImmutableMap.of(
              "logIdentifier",
              logIdentifier,
              "file",
              file.toString(),
              "failedAttempts",
              Integer.toString(re.getNumberOfFailedAttempts())
            )
          );
        } catch (Exception e) {
          metrics.error();
          LOG.warn("{} Couldn't upload or delete {}", logIdentifier, file, e);
          exceptionNotifier.notify(
            String.format("Error during upload (%s)", e.getMessage()),
            e,
            ImmutableMap.of("logIdentifier", logIdentifier, "file", file.toString())
          );
        } finally {
          context.stop();
        }
      } else {
        LOG.debug("{} is in use by another process, will retry upload later", file);
      }
    }

    LOG.info(
      "{} Uploaded {} out of {} item(s) in {}",
      logIdentifier,
      success,
      toUpload.size(),
      JavaUtils.duration(start)
    );
    return toUpload.size();
  }

  Path getMetadataPath() {
    return metadataPath;
  }

  S3UploadMetadata getUploadMetadata() {
    return uploadMetadata;
  }

  int upload(boolean isFinished) throws IOException {
    return uploadBatch(filesToUpload(isFinished));
  }

  @SuppressFBWarnings(
    value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
    justification = "https://github.com/spotbugs/spotbugs/issues/259"
  )
  List<Path> filesToUpload(boolean isFinished) throws IOException {
    final List<Path> toUpload = Lists.newArrayList();

    final Path directory = Paths.get(fileDirectory);

    if (!Files.exists(directory)) {
      LOG.info("Path {} doesn't exist", fileDirectory);
      return Collections.emptyList();
    }

    try (Stream<Path> paths = Files.walk(directory, 1)) {
      paths.forEach(file -> {
        if (file.equals(directory)) {
          return;
        }
        try {
          handleFile(file, isFinished, toUpload);
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
      });
    } catch (UncheckedIOException | NoSuchFileException nsfe) {
      LOG.debug("Parent file {} did not exist, skipping", directory);
    }
    return toUpload;
  }

  @SuppressFBWarnings(
    value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
    justification = "https://github.com/spotbugs/spotbugs/issues/259"
  )
  private AtomicInteger handleFile(Path path, boolean isFinished, List<Path> toUpload)
    throws IOException {
    AtomicInteger found = new AtomicInteger();
    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
      if (uploadMetadata.isCheckSubdirectories()) {
        LOG.trace("{} was a directory, checking files in directory", path);
        try (Stream<Path> paths = Files.walk(path, 1)) {
          paths.forEach(file -> {
            if (file.equals(path)) {
              return; // Files.walk includes an element that is the starting path itself, skip this
            }
            try {
              found.getAndAdd(handleFile(file, isFinished, toUpload).get());
            } catch (IOException ioe) {
              throw new RuntimeException(ioe);
            }
          });
        }
      } else {
        LOG.trace("{} was a directory, skipping", path);
      }
      return found;
    }

    if (!pathMatcher.matches(path.getFileName())) {
      if (
        !isFinished ||
        !finishedPathMatcher.isPresent() ||
        !finishedPathMatcher.get().matches(path.getFileName())
      ) {
        LOG.trace(
          "{} Skipping {} because it doesn't match {}",
          logIdentifier,
          path,
          uploadMetadata.getFileGlob()
        );
        return found;
      } else {
        LOG.trace(
          "Not skipping file {} because it matched finish glob {}",
          path,
          uploadMetadata.getOnFinishGlob().get()
        );
      }
    }

    if (Files.size(path) == 0) {
      LOG.trace("{} Skipping {} because its size is 0", logIdentifier, path);
      return found;
    }

    found.incrementAndGet();

    if (uploadMetadata.isCompressBeforeUpload() && !path.toString().endsWith(".gz")) {
      try {
        LOG.trace("{} Compressing {}...", logIdentifier, path);
        long start = -1;
        if (LOG.isTraceEnabled()) {
          start = System.currentTimeMillis();
        }

        new SimpleProcessManager(LOG)
          .runCommand(
            ImmutableList.of(CompressionType.GZIP.getCommand(), path.toString())
          );

        if (LOG.isTraceEnabled()) {
          LOG.trace(
            "{} Compressed {} in {}ms",
            logIdentifier,
            path,
            System.currentTimeMillis() - start
          );
        }

        toUpload.add(Paths.get(path.toAbsolutePath().toString() + ".gz"));
      } catch (InterruptedException | ProcessFailedException e) {
        LOG.warn(
          "{} Skipping {} because we were unable to compress it",
          logIdentifier,
          path,
          e
        );
      }
    } else {
      toUpload.add(path);
    }

    return found;
  }

  private boolean isFileOpen(Path path, boolean useFuser) {
    try {
      checkFileOpenLock.lock();
      if (useFuser) {
        SimpleProcessManager fuser = new SimpleProcessManager(LOG);
        List<String> cmd = ImmutableList.of("fuser", path.toAbsolutePath().toString());
        int exitCode = fuser.getExitCode(cmd, CHECK_FILE_OPEN_TIMEOUT_MILLIS);
        return exitCode == 0;
      } else {
        SimpleProcessManager lsof = new SimpleProcessManager(LOG);
        List<String> cmd = ImmutableList.of("lsof", path.toAbsolutePath().toString());
        List<String> output = lsof.runCommandWithOutput(cmd, Sets.newHashSet(0, 1));
        for (String line : output) {
          if (line.contains(path.toAbsolutePath().toString())) {
            return true;
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Could not determine if file {} was in use, skipping", path, e);
      return true;
    } finally {
      checkFileOpenLock.unlock();
    }
    return false;
  }

  Optional<Long> readFileAttributeAsLong(
    Path file,
    String attribute,
    UserDefinedFileAttributeView view,
    List<String> knownAttributes
  ) {
    if (knownAttributes.contains(attribute)) {
      try {
        LOG.trace("Attempting to read attribute {}, from file {}", attribute, file);
        ByteBuffer buf = ByteBuffer.allocate(view.size(attribute));
        view.read(attribute, buf);
        buf.flip();
        String value = Charset.defaultCharset().decode(buf).toString();
        if (Strings.isNullOrEmpty(value)) {
          LOG.debug("No attrbiute {} found for file {}", attribute, file);
          return Optional.empty();
        }
        return Optional.of(Long.parseLong(value));
      } catch (Exception e) {
        LOG.error("Error getting extra file metadata for {}", file, e);
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }
  }

  boolean shouldApplyStorageClass(
    long fileSizeBytes,
    Optional<String> maybeStorageClass
  ) {
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
        UserDefinedFileAttributeView view = Files.getFileAttributeView(
          file,
          UserDefinedFileAttributeView.class
        );
        List<String> attributes = view.list();
        LOG.debug("Found file attributes {} for file {}", attributes, file);
        Optional<Long> maybeStartTime = readFileAttributeAsLong(
          file,
          LOG_START_TIME_ATTR,
          view,
          attributes
        );
        Optional<Long> maybeEndTime = readFileAttributeAsLong(
          file,
          LOG_END_TIME_ATTR,
          view,
          attributes
        );
        return new UploaderFileAttributes(maybeStartTime, maybeEndTime);
      } catch (Exception e) {
        LOG.error("Could not get extra file metadata for {}", file, e);
      }
    }
    return new UploaderFileAttributes(Optional.empty(), Optional.empty());
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

    if (!Objects.equals(uploadMetadata, that.uploadMetadata)) {
      return false;
    }
    if (!Objects.equals(fileDirectory, that.fileDirectory)) {
      return false;
    }
    if (!Objects.equals(bucketName, that.bucketName)) {
      return false;
    }
    if (!Objects.equals(metadataPath, that.metadataPath)) {
      return false;
    }
    if (!Objects.equals(logIdentifier, that.logIdentifier)) {
      return false;
    }
    return Objects.equals(hostname, that.hostname);
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
