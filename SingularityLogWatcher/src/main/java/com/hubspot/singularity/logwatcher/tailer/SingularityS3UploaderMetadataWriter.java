package com.hubspot.singularity.logwatcher.tailer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.TailMetadata;

public class SingularityS3UploaderMetadataWriter {

  private final SingularityLogWatcherConfiguration configuration;
  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityS3UploaderMetadataWriter(SingularityLogWatcherConfiguration configuration, @Named(SingularityRunnerBaseModule.JSON_MAPPER) ObjectMapper objectMapper) {
    this.configuration = configuration;
    this.objectMapper = objectMapper;
  }

  private String getGlob(TailMetadata tailMetadata) {
    return String.format("%s*.gz*", Paths.get(tailMetadata.getFilename()).getFileName());
  }
  
  private String getS3KeyPattern(TailMetadata tailMetadata) {
    return String.format("%s/%s", tailMetadata.getTag(), configuration.getS3KeyPattern());
  }
  
  public void writeS3MetadataFile(TailMetadata tailMetadata, Path logrotatedFile) throws IOException {
    String logrotateToDirectory = configuration.getLogrotateToDirectory();
    Path directoryPath = logrotatedFile.getParent().resolve(logrotateToDirectory);
    
    String glob = getGlob(tailMetadata);
    
    S3UploadMetadata s3UploadMetadata = new S3UploadMetadata(directoryPath.toString(), glob, configuration.getS3Bucket(), getS3KeyPattern(tailMetadata));
    
    String s3UploadMetadatafilename = String.format("%s%s", tailMetadata.getFilenameKey(), configuration.getS3MetadataSuffix());
    
    Path s3UploadMetadataPath = configuration.getS3MetadataDirectory().resolve(s3UploadMetadatafilename);
    
    byte[] bytes = objectMapper.writeValueAsBytes(s3UploadMetadata);
    
    Files.write(s3UploadMetadataPath, bytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }
  
}
