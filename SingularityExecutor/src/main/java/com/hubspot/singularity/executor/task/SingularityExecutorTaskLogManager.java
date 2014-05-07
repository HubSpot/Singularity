package com.hubspot.singularity.executor.task;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.executor.SimpleProcessManager;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.models.LogrotateTemplateContext;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.TailMetadata;

public class SingularityExecutorTaskLogManager extends SimpleProcessManager {

  private final SingularityExecutorTask task;
  private final ObjectMapper objectMapper;
  private final TemplateManager templateManager;
  private final SingularityExecutorConfiguration configuration;
  
  public SingularityExecutorTaskLogManager(SingularityExecutorTask task, ObjectMapper objectMapper, TemplateManager templateManager, SingularityExecutorConfiguration configuration) {
    super(task.getLog(), task.getExecutorBashOut());
    this.task = task;
    this.objectMapper = objectMapper;
    this.templateManager = templateManager;
    this.configuration = configuration;
  }

  public void setup() {
    writeLogrotateFile();
    writeTailMetadata(false);
    writeS3MetadataFile(false);
  }
  
  private void writeLogrotateFile() {
    task.getLog().info("Writing logrotate configuration file to {}", getLogrotateConfPath());
    templateManager.writeLogrotateFile(getLogrotateConfPath(), new LogrotateTemplateContext(configuration, task.getServiceLogOut().toString()));
  }
 
  public void teardown() {
    writeTailMetadata(true);
    
    if (manualLogrotate()) {
      removeLogrotateFile();
      writeS3MetadataFile(true);
    }
  }
  
  private void removeLogrotateFile() {
    boolean deleted = false;
    try {
      deleted = Files.deleteIfExists(getLogrotateConfPath());
    } catch (Throwable t) {
      task.getLog().trace("Couldn't delete {}", getLogrotateConfPath(), t);
    }
    task.getLog().trace("Deleted {} : {}", getLogrotateConfPath(), deleted);
  }
  
  public boolean manualLogrotate() {
    final List<String> command = ImmutableList.of(
        configuration.getLogrotateCommand(), 
        "-f", 
        getLogrotateConfPath().toString());
    
    try {
      super.runCommand(command);
      return true;
    } catch (Throwable t) {
      task.getLog().warn("Tried to manually logrotate using {}, but caught", getLogrotateConfPath(), t);
      return false;
    }
  }
  
  private void ensureServiceOutExists() {
    try {
      Files.createFile(task.getServiceLogOut());
    } catch (FileAlreadyExistsException faee) {
      task.getLog().warn("Executor out {} already existed", task.getServiceLogOut());
    } catch (Throwable t) {
      task.getLog().error("Failed creating executor out {}", task.getServiceLogOut(), t);
    }
  }
  
  private void writeTailMetadata(boolean finished) {
    if (!task.getExecutorData().getLoggingTag().isPresent()) {
      if (!finished) {
        task.getLog().warn("Not writing logging metadata because logging tag is absent");
      }
      return;
    }
    
    if (!finished) {
      ensureServiceOutExists();
    }
    
    final TailMetadata tailMetadata = new TailMetadata(task.getServiceLogOut().toString(), task.getExecutorData().getLoggingTag().get(), task.getExecutorData().getLoggingExtraFields(), finished);
    final Path path = TailMetadata.getTailMetadataPath(configuration.getLogMetadataDirectory(), configuration.getLogMetadataSuffix(), tailMetadata);
    
    writeObject(tailMetadata, path);
  }
  
  private String getS3Glob() {
    return String.format("%s*.gz*", task.getServiceLogOut().getFileName());
  }
  
  private String getS3KeyPattern() {
    String s3KeyPattern = configuration.getS3KeyPattern();
    
    final SingularityTaskId singularityTaskId = getSingularityTaskId();
    
    s3KeyPattern = s3KeyPattern.replace("%tag", task.getExecutorData().getLoggingTag().or(""));
    s3KeyPattern = s3KeyPattern.replace("%requestId", singularityTaskId.getRequestId());
    s3KeyPattern = s3KeyPattern.replace("%deployId", singularityTaskId.getDeployId());
    s3KeyPattern = s3KeyPattern.replace("%host", singularityTaskId.getHost());
    s3KeyPattern = s3KeyPattern.replace("%taskId", task.getTaskId());
    
    return s3KeyPattern;
  }
  
  private SingularityTaskId getSingularityTaskId() {
    return SingularityTaskId.fromString(task.getTaskId());
  }
  
  public Path getLogrotateConfPath() {
    return configuration.getLogrotateConfDirectory().resolve(task.getTaskId());
  }

  private void writeS3MetadataFile(boolean finished) {
    Path logrotateDirectory = task.getServiceLogOut().getParent().resolve(configuration.getLogrotateToDirectory());
    
    S3UploadMetadata s3UploadMetadata = new S3UploadMetadata(logrotateDirectory.toString(), getS3Glob(), configuration.getS3Bucket(), getS3KeyPattern(), finished);
    
    String s3UploadMetadatafilename = String.format("%s%s", task.getTaskId(), configuration.getS3MetadataSuffix());
    
    Path s3UploadMetadataPath = configuration.getS3MetadataDirectory().resolve(s3UploadMetadatafilename);
    
    writeObject(s3UploadMetadata, s3UploadMetadataPath);
  }

  private void writeObject(Object o, Path path) {
    try {
      final byte[] bytes = objectMapper.writeValueAsBytes(o);
      
      task.getLog().info("Writing {} bytes of {} to {}", new Object[] { Integer.toString(bytes.length), o.toString(), path.toString() });
        
      Files.write(path, bytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (Throwable t) {
      task.getLog().error("Failed writing {}", o.toString(), t);
    }
  }
  
}
