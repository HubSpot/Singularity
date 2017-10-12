package com.hubspot.singularity.s3uploader;

import java.io.File;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.codahale.metrics.Timer.Context;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityS3FormatHelper;
import com.hubspot.singularity.SingularityS3Log;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderConfiguration;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderContentHeaders;

public class SingularityS3Uploader {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityS3Uploader.class);
  private static final String LOG_START_TIME_ATTR = "logstart";
  private static final String LOG_END_TIME_ATTR = "logend";


  private final S3UploadMetadata uploadMetadata;
  private final PathMatcher pathMatcher;
  private final Optional<PathMatcher> finishedPathMatcher;
  private final String fileDirectory;
  private final AmazonS3 s3Client;
  private final String s3BucketName;
  private final Path metadataPath;
  private final SingularityS3UploaderMetrics metrics;
  private final String logIdentifier;
  private final String hostname;
  private final SingularityS3UploaderConfiguration configuration;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;

  public SingularityS3Uploader(BasicAWSCredentials defaultCredentials, S3UploadMetadata uploadMetadata, FileSystem fileSystem, SingularityS3UploaderMetrics metrics, Path metadataPath,
                               SingularityS3UploaderConfiguration configuration, String hostname, SingularityRunnerExceptionNotifier exceptionNotifier) {
    BasicAWSCredentials credentials = defaultCredentials;

    if (uploadMetadata.getS3SecretKey().isPresent() && uploadMetadata.getS3AccessKey().isPresent()) {
      credentials = new BasicAWSCredentials(uploadMetadata.getS3AccessKey().get(), uploadMetadata.getS3SecretKey().get());
    }

    this.s3Client = new AmazonS3Client(credentials);
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
    this.s3BucketName = uploadMetadata.getS3Bucket();
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
      found += handleFile(file, isFinished, synchronizedToUpload, toUpload);
    }

    if (toUpload.isEmpty()) {
      return found;
    }

    uploadBatch(toUpload);

    return found;
  }

  private int handleFile(Path path, boolean isFinished, Set<Path> synchronizedToUpload, List<Path> toUpload) throws IOException {
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
        } catch (AmazonS3Exception se) {
          metrics.error();
          LOG.warn("{} Couldn't upload {} due to {} - {}", logIdentifier, file, se.getErrorCode(), se.getErrorMessage(), se);
          exceptionNotifier.notify(String.format("S3ServiceException during upload (%s)", se.getMessage()), se, ImmutableMap.of("logIdentifier", logIdentifier, "file", file.toString(), "errorCode", se.getErrorCode(), "errorMessage", se.getErrorMessage()));
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
      .retryIfExceptionOfType(AmazonS3Exception.class)
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

    private boolean shouldApplyStorageClass(long fileSizeBytes) {
      if (!uploadMetadata.getS3StorageClass().isPresent()) {
        return false;
      }

      if (!uploadMetadata.getApplyStorageClassIfOverBytes().isPresent()) {
        return true;
      }

      return fileSizeBytes > uploadMetadata.getApplyStorageClassIfOverBytes().get();
    }

    @Override
    public Boolean call() throws Exception {
      final long start = System.currentTimeMillis();

      final String key = SingularityS3FormatHelper.getKey(uploadMetadata.getS3KeyFormat(), sequence, Files.getLastModifiedTime(file).toMillis(), Objects.toString(file.getFileName()), hostname);

      long fileSizeBytes = Files.size(file);
      LOG.info("{} Uploading {} to {}/{} (size {})", logIdentifier, file, s3BucketName, key, fileSizeBytes);

      try {
        ObjectMetadata objectMetadata = new ObjectMetadata();

        Set<String> supportedViews = FileSystems.getDefault().supportedFileAttributeViews();
        LOG.trace("Supported attribute views are {}", supportedViews);
        if (supportedViews.contains("user")) {
          try {
            UserDefinedFileAttributeView view = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
            List<String> attributes = view.list();
            LOG.debug("Found file attributes {} for file {}", attributes, file);
            Optional<Long> maybeStartTime = readFileAttributeAsLong(LOG_START_TIME_ATTR, view, attributes);
            if (maybeStartTime.isPresent()) {
              objectMetadata.addUserMetadata(SingularityS3Log.LOG_START_S3_ATTR, maybeStartTime.get().toString());
              LOG.debug("Added extra metadata for object ({}:{})", SingularityS3Log.LOG_START_S3_ATTR, maybeStartTime.get());
            }
            Optional<Long> maybeEndTime = readFileAttributeAsLong(LOG_END_TIME_ATTR, view, attributes);
            if (maybeEndTime.isPresent()) {
              objectMetadata.addUserMetadata(SingularityS3Log.LOG_END_S3_ATTR, maybeEndTime.get().toString());
              LOG.debug("Added extra metadata for object ({}:{})", SingularityS3Log.LOG_END_S3_ATTR, maybeEndTime.get());
            }
          } catch (Exception e) {
            LOG.error("Could not get extra file metadata for {}", file, e);
          }
        }

        for (SingularityS3UploaderContentHeaders contentHeaders : configuration.getS3ContentHeaders()) {
          if (file.toString().endsWith(contentHeaders.getFilenameEndsWith())) {
            LOG.debug("{} Using content headers {} for file {}", logIdentifier, contentHeaders, file);
            if (contentHeaders.getContentType().isPresent()) {
              objectMetadata.setContentType(contentHeaders.getContentType().get());
            }
            if (contentHeaders.getContentEncoding().isPresent()) {
              objectMetadata.setContentEncoding(contentHeaders.getContentEncoding().get());
            }
            break;
          }
        }

        Optional<StorageClass> maybeStorageClass = Optional.absent();

        if (shouldApplyStorageClass(fileSizeBytes)) {
          LOG.debug("{} adding storage class {} to {}", logIdentifier, uploadMetadata.getS3StorageClass().get(), file);
          maybeStorageClass = Optional.of(StorageClass.fromValue(uploadMetadata.getS3StorageClass().get()));
        }

        LOG.debug("Uploading object with metadata {}", objectMetadata);

        if (fileSizeBytes > configuration.getMaxSingleUploadSizeBytes()) {
          multipartUpload(key, file.toFile(), objectMetadata, maybeStorageClass);
        } else {
          PutObjectRequest putObjectRequest = new PutObjectRequest(s3BucketName, key, file.toFile()).withMetadata(objectMetadata);
          if (maybeStorageClass.isPresent()) {
            putObjectRequest.setStorageClass(maybeStorageClass.get());
          }
          s3Client.putObject(putObjectRequest);
        }
      } catch (Exception e) {
        LOG.warn("Exception uploading {}", file, e);
        throw e;
      }

      LOG.info("{} Uploaded {} in {}", logIdentifier, key, JavaUtils.duration(start));

      return true;
    }

    private Optional<Long> readFileAttributeAsLong(String attribute, UserDefinedFileAttributeView view, List<String> knownAttributes) {
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

  private void multipartUpload(String key, File file, ObjectMetadata objectMetadata, Optional<StorageClass> maybeStorageClass) throws Exception {
    List<PartETag> partETags = new ArrayList<>();
    InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(s3BucketName, key, objectMetadata);
    if (maybeStorageClass.isPresent()) {
      initRequest.setStorageClass(maybeStorageClass.get());
    }
    InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

    long contentLength = file.length();
    long partSize = configuration.getUploadPartSize();

    try {
      long filePosition = 0;
      for (int i = 1; filePosition < contentLength; i++) {
        partSize = Math.min(partSize, (contentLength - filePosition));
        UploadPartRequest uploadRequest = new UploadPartRequest()
            .withBucketName(s3BucketName)
            .withKey(key)
            .withUploadId(initResponse.getUploadId())
            .withPartNumber(i)
            .withFileOffset(filePosition)
            .withFile(file)
            .withPartSize(partSize);
        partETags.add(s3Client.uploadPart(uploadRequest).getPartETag());
        filePosition += partSize;
      }

      CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(s3BucketName, key, initResponse.getUploadId(), partETags);
      s3Client.completeMultipartUpload(completeRequest);
    } catch (Exception e) {
      s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(s3BucketName, key, initResponse.getUploadId()));
      Throwables.propagate(e);
    }
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
    if (s3BucketName != null ? !s3BucketName.equals(that.s3BucketName) : that.s3BucketName != null) {
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
    result = 31 * result + (s3BucketName != null ? s3BucketName.hashCode() : 0);
    result = 31 * result + (metadataPath != null ? metadataPath.hashCode() : 0);
    result = 31 * result + (logIdentifier != null ? logIdentifier.hashCode() : 0);
    result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
    return result;
  }
}
