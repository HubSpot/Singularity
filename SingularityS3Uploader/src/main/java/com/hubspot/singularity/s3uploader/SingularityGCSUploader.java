package com.hubspot.singularity.s3uploader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityS3FormatHelper;
import com.hubspot.singularity.SingularityS3Log;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.runner.base.shared.JsonObjectFileHelper;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderConfiguration;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderContentHeaders;

public class SingularityGCSUploader extends SingularityUploader {
  private final Storage storage;

  public SingularityGCSUploader(S3UploadMetadata uploadMetadata, FileSystem fileSystem, SingularityS3UploaderMetrics metrics, Path metadataPath,
                                SingularityS3UploaderConfiguration configuration, String hostname, SingularityRunnerExceptionNotifier exceptionNotifier,
                                JsonObjectFileHelper jsonHelper) {
    super(uploadMetadata, fileSystem, metrics, metadataPath, configuration, hostname, exceptionNotifier);
    this.storage = StorageOptions.newBuilder()
        .setCredentials(loadCredentials(uploadMetadata, jsonHelper))
        .build()
        .getService();
  }

  public static GoogleCredentials loadCredentials(S3UploadMetadata uploadMetadata, JsonObjectFileHelper jsonHelper) {
    try {
      if (!uploadMetadata.getGcsCredentials().isEmpty()) {
        return GoogleCredentials.fromStream(jsonHelper.toInputStream(uploadMetadata.getGcsCredentials()));
      }

      // Load from default credentials as determined by GOOGLE_APPLICATION_CREDENTIALS var if none provided in metadata
      return GoogleCredentials.getApplicationDefault();
    } catch (IOException e) {
      throw new RuntimeException("Issue reading credentials file specified in `GOOGLE_APPLICATION_CREDENTIALS`", e);
    }
  }

  @Override
  protected  void uploadSingle(int sequence, Path file) throws Exception {
    Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
        .retryIfExceptionOfType(RuntimeException.class)
        .retryIfRuntimeException()
        .withWaitStrategy(WaitStrategies.fixedWait(configuration.getRetryWaitMs(), TimeUnit.MILLISECONDS))
        .withStopStrategy(StopStrategies.stopAfterAttempt(configuration.getRetryCount()))
        .build();

    retryer.call(() -> {
      final long start = System.currentTimeMillis();

      final String key = SingularityS3FormatHelper.getKey(uploadMetadata.getS3KeyFormat(), sequence, Files.getLastModifiedTime(file).toMillis(), Objects.toString(file.getFileName()), hostname);

      long fileSizeBytes = Files.size(file);
      LOG.info("{} Uploading {} to {}/{} (size {})", logIdentifier, file, bucketName, key, fileSizeBytes);

      BlobInfo.Builder blobInfoBuilder = BlobInfo.newBuilder(bucketName, key);

      UploaderFileAttributes fileAttributes = getFileAttributes(file);

      Map<String, String> metadata = new HashMap<>();
      if (fileAttributes.getStartTime().isPresent()) {
        metadata.put(SingularityS3Log.LOG_START_S3_ATTR, fileAttributes.getStartTime().get().toString());
        LOG.debug("Added extra metadata for object ({}:{})", SingularityS3Log.LOG_START_S3_ATTR, fileAttributes.getStartTime().get());
      }
      if (fileAttributes.getEndTime().isPresent()) {
        metadata.put(SingularityS3Log.LOG_START_S3_ATTR, fileAttributes.getEndTime().get().toString());
        LOG.debug("Added extra metadata for object ({}:{})", SingularityS3Log.LOG_END_S3_ATTR, fileAttributes.getEndTime().get());
      }

      blobInfoBuilder.setMetadata(metadata);

      for (SingularityS3UploaderContentHeaders contentHeaders : configuration.getS3ContentHeaders()) {
        if (file.toString().endsWith(contentHeaders.getFilenameEndsWith())) {
          LOG.debug("{} Using content headers {} for file {}", logIdentifier, contentHeaders, file);
          if (contentHeaders.getContentType().isPresent()) {
            blobInfoBuilder.setContentType(contentHeaders.getContentType().get());
          }
          if (contentHeaders.getContentEncoding().isPresent()) {
            blobInfoBuilder.setContentEncoding(contentHeaders.getContentEncoding().get());
          }
          break;
        }
      }

      if (shouldApplyStorageClass(fileSizeBytes, uploadMetadata.getGcsStorageClass())) {
        LOG.debug("{} adding storage class {} to {}", logIdentifier, uploadMetadata.getGcsStorageClass().get(), file);
        blobInfoBuilder.setStorageClass(StorageClass.valueOf(uploadMetadata.getGcsStorageClass().get()));
      }

      try (FileInputStream fileInputStream = new FileInputStream(file.toFile())){
        storage.create(blobInfoBuilder.build(), fileInputStream);
        LOG.info("{} Uploaded {} in {}", logIdentifier, key, JavaUtils.duration(start));
        return true;
      } catch (StorageException se) {
        LOG.warn("{} Couldn't upload {} due to  {}", logIdentifier, file, se.getMessage(), se);
        throw se;
      } catch (Exception e) {
        LOG.warn("Exception uploading {}", file, e);
        throw e;
      }
    });
  }
}
