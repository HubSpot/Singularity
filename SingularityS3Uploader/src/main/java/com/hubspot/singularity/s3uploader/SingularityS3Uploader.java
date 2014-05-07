package com.hubspot.singularity.s3uploader;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.jets3t.service.S3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;

public class SingularityS3Uploader {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityS3Uploader.class);

  private final S3UploadMetadata uploadMetadata;
  private final PathMatcher pathMatcher;
  private final String fileDirectory;
  private final S3Service s3Service;
  private final S3Bucket s3Bucket;
  private final Path metadataPath;
  private final String logIdentifier;
  
  public SingularityS3Uploader(S3Service s3Service, S3UploadMetadata uploadMetadata, FileSystem fileSystem, Path metadataPath) {
    this.s3Service = s3Service;
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
    
    for (Path file : JavaUtils.iterable(Paths.get(fileDirectory))) {
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
    
    uploadBatch(toUpload);
    
    return found;
  }
  
  private void uploadBatch(List<Path> toUpload) {
    final long start = System.currentTimeMillis();
    LOG.info("{} Uploading {} item(s)", logIdentifier, toUpload.size());
    
    int success = 0;
    
    for (int i = 0; i < toUpload.size(); i++) {
      final Path file = toUpload.get(i);
      try {
        uploadSingle(i, start, file);
        success++;
        Files.delete(file);
      } catch (Exception e) {
        LOG.warn("{} Couldn't upload or delete {}", logIdentifier, file, e);
      }
    }
    
    LOG.info("{} Uploaded {} out of {} item(s) in {}", logIdentifier, success, toUpload.size(), JavaUtils.duration(start));
  }
  
  private String getKey(int sequence, long timestamp, Path file) {
    final Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(timestamp);
    
    String s3KeyFormat = uploadMetadata.getS3KeyFormat();
    
    String filename = file.getFileName().toString();
    
    s3KeyFormat = s3KeyFormat.replace("%filename", filename);
    
    int lastPeriod = filename.lastIndexOf(".");
    
    if (lastPeriod > -1) {
      s3KeyFormat = s3KeyFormat.replace("%fileext", filename.substring(lastPeriod));
    }
    
    s3KeyFormat = s3KeyFormat.replace("%Y", Integer.toString(calendar.get(Calendar.YEAR)));
    s3KeyFormat = s3KeyFormat.replace("%m", Integer.toString(calendar.get(Calendar.MONTH)));
    s3KeyFormat = s3KeyFormat.replace("%d", Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)));
    s3KeyFormat = s3KeyFormat.replace("%s", Long.toString(timestamp));
    s3KeyFormat = s3KeyFormat.replace("%index", Integer.toString(sequence));
    
    return s3KeyFormat;
  }
  
  private void uploadSingle(int sequence, long timestamp, Path file) throws Exception {
    final long start = System.currentTimeMillis();
    final String key = getKey(sequence, timestamp, file);
    
    LOG.info("{} Uploading {} to {}-{} (size {})", logIdentifier, file, s3Bucket.getName(), key, Files.size(file));
    
    S3Object object = new S3Object(s3Bucket, file.toFile());
    object.setKey(key);
    
    s3Service.putObject(s3Bucket, object);
    
    LOG.info("{} Uploaded {} in {}", logIdentifier, key, JavaUtils.duration(start));
  }
  
}
