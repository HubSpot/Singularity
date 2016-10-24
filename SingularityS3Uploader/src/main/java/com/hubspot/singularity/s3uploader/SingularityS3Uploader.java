package com.hubspot.singularity.s3uploader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.utils.MultipartUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer.Context;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderContentHeaders;
import com.hubspot.singularity.SingularityS3FormatHelper;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderConfiguration;

public class SingularityS3Uploader implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityS3Uploader.class);

  private final S3UploadMetadata uploadMetadata;
  private final PathMatcher pathMatcher;
  private final Optional<PathMatcher> finishedPathMatcher;
  private final String fileDirectory;
  private final S3Service s3Service;
  private final S3Bucket s3Bucket;
  private final Path metadataPath;
  private final SingularityS3UploaderMetrics metrics;
  private final String logIdentifier;
  private final String hostname;
  private final SingularityS3UploaderConfiguration configuration;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;

  public SingularityS3Uploader(AWSCredentials defaultCredentials, S3UploadMetadata uploadMetadata, FileSystem fileSystem, SingularityS3UploaderMetrics metrics, Path metadataPath,
      SingularityS3UploaderConfiguration configuration, String hostname, SingularityRunnerExceptionNotifier exceptionNotifier) {
    AWSCredentials credentials = defaultCredentials;

    if (uploadMetadata.getS3SecretKey().isPresent() && uploadMetadata.getS3AccessKey().isPresent()) {
      credentials = new AWSCredentials(uploadMetadata.getS3AccessKey().get(), uploadMetadata.getS3SecretKey().get());
    }

    try {
      this.s3Service = new RestS3Service(credentials);
    } catch (S3ServiceException e) {
      throw Throwables.propagate(e);
    }

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
    this.s3Bucket = new S3Bucket(uploadMetadata.getS3Bucket());
    this.metadataPath = metadataPath;
    this.logIdentifier = String.format("[%s]", metadataPath.getFileName());
    this.configuration = configuration;
    this.exceptionNotifier = exceptionNotifier;
  }

  public Path getMetadataPath() {
    return metadataPath;
  }

  public S3UploadMetadata getUploadMetadata() {
    return uploadMetadata;
  }

  @Override
  public void close() throws IOException {
    try {
      s3Service.shutdown();
    } catch (ServiceException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String toString() {
    return "SingularityS3Uploader [uploadMetadata=" + uploadMetadata + ", metadataPath=" + metadataPath + "]";
  }

  public int upload(Set<Path> synchronizedToUpload, boolean isFinished) throws IOException {
    final List<Path> toUpload = Lists.newArrayList();
    int found = 0;

    final Path directory = Paths.get(fileDirectory);

    if (!Files.exists(directory)) {
      LOG.info("Path {} doesn't exist", fileDirectory);
      return found;
    }

    for (Path file : JavaUtils.iterable(directory)) {
      if (!pathMatcher.matches(file.getFileName())) {
        if (!isFinished || !finishedPathMatcher.isPresent() || !finishedPathMatcher.get().matches(file.getFileName())) {
          LOG.trace("{} Skipping {} because it doesn't match {}", logIdentifier, file, uploadMetadata.getFileGlob());
          continue;
        } else {
          LOG.trace("Not skipping file {} because it matched finish glob {}", file, uploadMetadata.getOnFinishGlob().get());
        }
      }

      if (Files.size(file) == 0) {
        LOG.trace("{} Skipping {} because its size is 0", logIdentifier, file);
        continue;
      }

      found++;

      if (synchronizedToUpload.add(file)) {
        toUpload.add(file);
      } else {
        LOG.debug("{} Another uploader already added {}", logIdentifier, file);
      }
    }

    if (toUpload.isEmpty()) {
      return found;
    }

    uploadBatch(toUpload);

    return found;
  }

  private void uploadBatch(List<Path> toUpload) {
    final long start = System.currentTimeMillis();
    LOG.info("{} Uploading {} item(s)", logIdentifier, toUpload.size());

    int success = 0;

    for (int i = 0; i < toUpload.size(); i++) {
      final Context context = metrics.getUploadTimer().time();
      final Path file = toUpload.get(i);
      if (!configuration.isCheckForOpenFiles() || !fileOpen(file)) {
        try {
          uploadSingle(i, file);
          metrics.upload();
          success++;
          Files.delete(file);
        } catch (S3ServiceException se) {
          metrics.error();
          LOG.warn("{} Couldn't upload {} due to {} ({}) - {}", logIdentifier, file, se.getErrorCode(), se.getResponseCode(), se.getErrorMessage(), se);
          exceptionNotifier.notify(String.format("S3ServiceException during upload (%s)", se.getMessage()), se, ImmutableMap.of("logIdentifier", logIdentifier, "file", file.toString(), "errorCode", se.getErrorCode(), "responseCode", Integer.toString(se.getResponseCode()), "errorMessage", se.getErrorMessage()));
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

  private void uploadSingle(int sequence, Path file) throws Exception {
    Callable<Boolean> uploader = new Uploader(sequence, file);

    Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
      .retryIfExceptionOfType(S3ServiceException.class)
      .retryIfRuntimeException()
      .withWaitStrategy(WaitStrategies.fixedWait(configuration.getRetryWaitMs(), TimeUnit.MILLISECONDS))
      .withStopStrategy(StopStrategies.stopAfterAttempt(configuration.getRetryCount()))
      .build();
    retryer.call(uploader);
  }

  class Uploader implements Callable<Boolean> {
    private final int sequence;
    private final Path file;

    public Uploader(int sequence, Path file) {
      this.file = file;
      this.sequence = sequence;
    }

    @Override
    public Boolean call() throws Exception {
      final long start = System.currentTimeMillis();

      final String key = SingularityS3FormatHelper.getKey(uploadMetadata.getS3KeyFormat(), sequence, Files.getLastModifiedTime(file).toMillis(), Objects.toString(file.getFileName()), hostname);

      long fileSizeBytes = Files.size(file);
      LOG.info("{} Uploading {} to {}/{} (size {})", logIdentifier, file, s3Bucket.getName(), key, fileSizeBytes);

      try {
        S3Object object = new S3Object(s3Bucket, file.toFile());
        object.setKey(key);

        for (SingularityS3UploaderContentHeaders contentHeaders : configuration.getS3ContentHeaders()) {
          if (file.toString().endsWith(contentHeaders.getFilenameEndsWith())) {
            LOG.debug("{} Using content headers {} for file {}", logIdentifier, contentHeaders, file);
            if (contentHeaders.getContentType().isPresent()) {
              object.setContentType(contentHeaders.getContentType().get());
            }
            if (contentHeaders.getContentEncoding().isPresent()) {
              object.setContentEncoding(contentHeaders.getContentEncoding().get());
            }
            break;
          }
        }

        if (fileSizeBytes > configuration.getMaxSingleUploadSizeBytes()) {
          multipartUpload(object);
        } else {
          s3Service.putObject(s3Bucket, object);
        }
      } catch (Exception e) {
        LOG.warn("Exception uploading {}", file, e);
        throw e;
      }

      LOG.info("{} Uploaded {} in {}", logIdentifier, key, JavaUtils.duration(start));

      return true;
    }
  }

  public static boolean fileOpen(Path path) {
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

  private void multipartUpload(S3Object object) throws Exception {

    List<StorageObject> objectsToUploadAsMultipart = Arrays.<StorageObject>asList(object);

    MultipartUtils mpUtils = new MultipartUtils(configuration.getUploadPartSize());
    mpUtils.uploadObjects(s3Bucket.getName(), s3Service, objectsToUploadAsMultipart, null);
  }

}
