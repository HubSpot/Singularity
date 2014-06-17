package com.hubspot.singularity.executor.task;

import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.hubspot.singularity.SingularityS3FormatHelper;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.executor.SimpleProcessManager;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.models.LogrotateTemplateContext;
import com.hubspot.singularity.runner.base.shared.JsonObjectFileHelper;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.TailMetadata;

public class SingularityExecutorTaskLogManager extends SimpleProcessManager {

  private final SingularityExecutorTaskConfiguration taskConfiguration;
  private final TemplateManager templateManager;
  private final SingularityExecutorConfiguration configuration;
  private final Logger log;
  private final JsonObjectFileHelper jsonObjectFileHelper;
  
  public SingularityExecutorTaskLogManager(SingularityExecutorTaskConfiguration taskConfiguration, TemplateManager templateManager, SingularityExecutorConfiguration configuration, Logger log, JsonObjectFileHelper jsonObjectFileHelper) {
    super(log);
    this.log = log;
    this.taskConfiguration = taskConfiguration;
    this.templateManager = templateManager;
    this.configuration = configuration;
    this.jsonObjectFileHelper = jsonObjectFileHelper;
  }

  public void setup() {
    writeLogrotateFile();
    writeTailMetadata(false);
    writeS3MetadataFile(false);
  }
  
  private void writeLogrotateFile() {
    log.info("Writing logrotate configuration file to {}", getLogrotateConfPath());
    templateManager.writeLogrotateFile(getLogrotateConfPath(), new LogrotateTemplateContext(configuration, taskConfiguration.getServiceLogOut().toString()));
  }
   
  public boolean teardown() {
    boolean writeTailMetadataSuccess = writeTailMetadata(true);
    
    copyLogTail();
    
    if (manualLogrotate()) {
      boolean removeLogRotateFileSuccess = removeLogrotateFile();
      boolean writeS3MetadataFileSuccess = writeS3MetadataFile(true);
      
      return writeTailMetadataSuccess && removeLogRotateFileSuccess && writeS3MetadataFileSuccess;
    } else {
      return false;
    }
  }
  
  private void copyLogTail() {
    if (configuration.getTailLogLinesToSave() <= 0) {
      return;
    }
   
    final List<String> cmd = ImmutableList.of(
        "tail", 
        "-n", 
        Integer.toString(configuration.getTailLogLinesToSave()), 
        taskConfiguration.getServiceLogOut().toString());
    
    try {
      super.runCommand(cmd, Redirect.to(taskConfiguration.getTaskDirectory().resolve(configuration.getServiceFinishedTailLog()).toFile()));
    } catch (Throwable t) {
      log.error("Failed saving tail of log {} to {}", new Object[] { taskConfiguration.getServiceLogOut().toString(), configuration.getServiceFinishedTailLog(), t});
    }
  }
  
  private boolean removeLogrotateFile() {
    boolean deleted = false;
    try {
      deleted = Files.deleteIfExists(getLogrotateConfPath());
    } catch (Throwable t) {
      log.trace("Couldn't delete {}", getLogrotateConfPath(), t);
      return false;
    }
    log.trace("Deleted {} : {}", getLogrotateConfPath(), deleted);
    return true;
  }
  
  public boolean manualLogrotate() {
    final List<String> command = ImmutableList.of(
        configuration.getLogrotateCommand(), 
        "-f",
        "-s",
        configuration.getLogrotateStateFile(),
        getLogrotateConfPath().toString());
    
    try {
      super.runCommand(command);
      return true;
    } catch (Throwable t) {
      log.warn("Tried to manually logrotate using {}, but caught", getLogrotateConfPath(), t);
      return false;
    }
  }
  
  private void ensureServiceOutExists() {
    try {
      Files.createFile(taskConfiguration.getServiceLogOut());
    } catch (FileAlreadyExistsException faee) {
      log.warn("Executor out {} already existed", taskConfiguration.getServiceLogOut());
    } catch (Throwable t) {
      log.error("Failed creating executor out {}", taskConfiguration.getServiceLogOut(), t);
    }
  }
  
  private boolean writeTailMetadata(boolean finished) {
    if (!taskConfiguration.getExecutorData().getLoggingTag().isPresent()) {
      if (!finished) {
        log.warn("Not writing logging metadata because logging tag is absent");
      }
      return true;
    }
    
    if (!finished) {
      ensureServiceOutExists();
    }
    
    final TailMetadata tailMetadata = new TailMetadata(taskConfiguration.getServiceLogOut().toString(), taskConfiguration.getExecutorData().getLoggingTag().get(), taskConfiguration.getExecutorData().getLoggingExtraFields(), finished);
    final Path path = TailMetadata.getTailMetadataPath(configuration.getLogMetadataDirectory(), configuration.getLogMetadataSuffix(), tailMetadata);
    
    return jsonObjectFileHelper.writeObject(tailMetadata, path, log);
  }
  
  private String getS3Glob() {
    return String.format("%s*.gz*", taskConfiguration.getServiceLogOut().getFileName());
  }
  
  private String getS3KeyPattern() {
    String s3KeyPattern = configuration.getS3KeyPattern();
    
    final SingularityTaskId singularityTaskId = getSingularityTaskId();
    
    return SingularityS3FormatHelper.getS3KeyFormat(s3KeyPattern, singularityTaskId, taskConfiguration.getExecutorData().getLoggingTag());
  }
  
  private SingularityTaskId getSingularityTaskId() {
    return SingularityTaskId.fromString(taskConfiguration.getTaskId());
  }
  
  public Path getLogrotateConfPath() {
    return configuration.getLogrotateConfDirectory().resolve(taskConfiguration.getTaskId());
  }

  private boolean writeS3MetadataFile(boolean finished) {
    Path logrotateDirectory = taskConfiguration.getServiceLogOut().getParent().resolve(configuration.getLogrotateToDirectory());
    
    S3UploadMetadata s3UploadMetadata = new S3UploadMetadata(logrotateDirectory.toString(), getS3Glob(), configuration.getS3Bucket(), getS3KeyPattern(), finished);
    
    String s3UploadMetadatafilename = String.format("%s%s", taskConfiguration.getTaskId(), configuration.getS3MetadataSuffix());
    
    Path s3UploadMetadataPath = configuration.getS3MetadataDirectory().resolve(s3UploadMetadatafilename);
    
    return jsonObjectFileHelper.writeObject(s3UploadMetadata, s3UploadMetadataPath, log);
  }
  
}
