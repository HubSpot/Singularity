package com.hubspot.singularity.s3uploader;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.jets3t.service.S3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer.Context;
import com.google.common.collect.Lists;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityS3FormatHelper;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;

public class SingularityS3Uploader {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityS3Uploader.class);

  private final S3UploadMetadata uploadMetadata;
  private final PathMatcher pathMatcher;
  private final String fileDirectory;
  private final S3Service s3Service;
  private final S3Bucket s3Bucket;
  private final Path metadataPath;
  private final SingularityS3UploaderMetrics metrics;
  private final String logIdentifier;

  public SingularityS3Uploader(S3Service s3Service, S3UploadMetadata uploadMetadata, FileSystem fileSystem, SingularityS3UploaderMetrics metrics, Path metadataPath) {
    this.s3Service = s3Service;
    this.metrics = metrics;
    this.uploadMetadata = uploadMetadata;
    this.fileDirectory = uploadMetadata.getDirectory();
    this.pathMatcher = fileSystem.getPathMatcher("glob:" + uploadMetadata.getFileGlob());
    this.s3Bucket = new S3Bucket(uploadMetadata.getS3Bucket());
    this.metadataPath = metadataPath;
    this.logIdentifier = String.format("[%s]", metadataPath.getFileName());
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

  public int upload(Set<Path> synchronizedToUpload) throws IOException {
    final List<Path> toUpload = Lists.newArrayList();
    int found = 0;

    final Path directory = Paths.get(fileDirectory);

    if (!Files.exists(directory)) {
      LOG.info("Path {} doesn't exist", fileDirectory);
      return found;
    }

    for (Path file : JavaUtils.iterable(directory)) {
      if (!pathMatcher.matches(file.getFileName())) {
        LOG.trace("{} Skipping {} because it didn't match {}", logIdentifier, file, uploadMetadata.getFileGlob());
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
      try {
        uploadSingle(i, file);
        metrics.upload();
        success++;
        Files.delete(file);
      } catch (Exception e) {
        metrics.error();
        LOG.warn("{} Couldn't upload or delete {}", logIdentifier, file, e);
      } finally {
        context.stop();
      }
    }

    LOG.info("{} Uploaded {} out of {} item(s) in {}", logIdentifier, success, toUpload.size(), JavaUtils.duration(start));
  }

  private void uploadSingle(int sequence, Path file) throws Exception {
    final long start = System.currentTimeMillis();

    final String key = SingularityS3FormatHelper.getKey(uploadMetadata.getS3KeyFormat(), sequence, Files.getLastModifiedTime(file).toMillis(), file.getFileName().toString());

    LOG.info("{} Uploading {} to {}/{} (size {})", logIdentifier, file, s3Bucket.getName(), key, Files.size(file));

    S3Object object = new S3Object(s3Bucket, file.toFile());
    object.setKey(key);

    s3Service.putObject(s3Bucket, object);

    LOG.info("{} Uploaded {} in {}", logIdentifier, key, JavaUtils.duration(start));
  }

}
