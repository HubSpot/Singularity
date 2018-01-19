package com.hubspot.singularity.s3uploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Preconditions;
import com.hubspot.singularity.SingularityS3FormatHelper;
import com.hubspot.singularity.SingularityS3Log;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderConfiguration;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderContentHeaders;

public class SingularityGCSUploader extends SingularityUploader {
  private final Storage storage;

  public SingularityGCSUploader(S3UploadMetadata uploadMetadata, FileSystem fileSystem, SingularityS3UploaderMetrics metrics, Path metadataPath,
                                SingularityS3UploaderConfiguration configuration, String hostname, SingularityRunnerExceptionNotifier exceptionNotifier) {
    super(uploadMetadata, fileSystem, metrics, metadataPath, configuration, hostname, exceptionNotifier);
    Preconditions.checkState(configuration.getGcsCredentialsPath().isPresent());
    this.storage = StorageOptions.newBuilder().setCredentials(loadCredentials(configuration.getGcsCredentialsPath().get())).build().getService();
  }

  public static ServiceAccountCredentials loadCredentials(String path) {
    File credentialsPath = new File(path);
    if (!credentialsPath.exists()) {
      throw new RuntimeException(String.format("Service account file %s does not exist.", credentialsPath.getName()));
    }
    try (FileInputStream serviceAccountStream = new FileInputStream(credentialsPath)) {
      return ServiceAccountCredentials.fromStream(serviceAccountStream);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(String.format("Unable to find service account file '%s'.", path));
    } catch (IOException e) {
      throw new RuntimeException(String.format("Issue reading service account file '%s', please check permission of the file", path));
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

      if (shouldApplyStorageClass(fileSizeBytes)) {
        LOG.debug("{} adding storage class {} to {}", logIdentifier, uploadMetadata.getS3StorageClass().get(), file);
        blobInfoBuilder.setStorageClass(StorageClass.valueOf(uploadMetadata.getS3StorageClass().get()));
      }

      storage.create(blobInfoBuilder.build(), new FileInputStream(file.toFile()));

      return true;
    });
  }
}
