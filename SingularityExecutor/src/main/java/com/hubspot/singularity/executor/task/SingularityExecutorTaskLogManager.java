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
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.TailMetadata;

public class SingularityExecutorTaskLogManager extends SimpleProcessManager {

  private final SingularityExecutorTaskDefinition taskDefiniton;
  private final TemplateManager templateManager;
  private final SingularityExecutorConfiguration configuration;
  private final Logger log;
  private final ExecutorUtils executorUtils;
  
  public SingularityExecutorTaskLogManager(SingularityExecutorTaskDefinition taskDefiniton, TemplateManager templateManager, SingularityExecutorConfiguration configuration, Logger log, ExecutorUtils executorUtils) {
    super(log);
    this.log = log;
    this.taskDefiniton = taskDefiniton;
    this.templateManager = templateManager;
    this.configuration = configuration;
    this.executorUtils = executorUtils;
  }

  public void setup() {
    writeLogrotateFile();
    writeTailMetadata(false);
    writeS3MetadataFile(false);
  }
  
  private void writeLogrotateFile() {
    log.info("Writing logrotate configuration file to {}", getLogrotateConfPath());
    templateManager.writeLogrotateFile(getLogrotateConfPath(), new LogrotateTemplateContext(configuration, taskDefiniton.getServiceLogOut().toString()));
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
        taskDefiniton.getServiceLogOut().toString());
    
    try {
      super.runCommand(cmd, Redirect.to(taskDefiniton.getTaskDirectory().resolve(configuration.getServiceFinishedTailLog()).toFile()));
    } catch (Throwable t) {
      log.error("Failed saving tail of log {} to {}", new Object[] { taskDefiniton.getServiceLogOut().toString(), configuration.getServiceFinishedTailLog(), t});
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
      Files.createFile(taskDefiniton.getServiceLogOut());
    } catch (FileAlreadyExistsException faee) {
      log.warn("Executor out {} already existed", taskDefiniton.getServiceLogOut());
    } catch (Throwable t) {
      log.error("Failed creating executor out {}", taskDefiniton.getServiceLogOut(), t);
    }
  }
  
  private boolean writeTailMetadata(boolean finished) {
    if (!taskDefiniton.getExecutorData().getLoggingTag().isPresent()) {
      if (!finished) {
        log.warn("Not writing logging metadata because logging tag is absent");
      }
      return true;
    }
    
    if (!finished) {
      ensureServiceOutExists();
    }
    
    final TailMetadata tailMetadata = new TailMetadata(taskDefiniton.getServiceLogOut().toString(), taskDefiniton.getExecutorData().getLoggingTag().get(), taskDefiniton.getExecutorData().getLoggingExtraFields(), finished);
    final Path path = TailMetadata.getTailMetadataPath(configuration.getLogMetadataDirectory(), configuration.getLogMetadataSuffix(), tailMetadata);
    
    return writeObject(tailMetadata, path);
  }
  
  private String getS3Glob() {
    return String.format("%s*.gz*", taskDefiniton.getServiceLogOut().getFileName());
  }
  
  private String getS3KeyPattern() {
    String s3KeyPattern = configuration.getS3KeyPattern();
    
    final SingularityTaskId singularityTaskId = getSingularityTaskId();
    
    return SingularityS3FormatHelper.getS3KeyFormat(s3KeyPattern, singularityTaskId, taskDefiniton.getExecutorData().getLoggingTag());
  }
  
  private SingularityTaskId getSingularityTaskId() {
    return SingularityTaskId.fromString(taskDefiniton.getTaskId());
  }
  
  public Path getLogrotateConfPath() {
    return configuration.getLogrotateConfDirectory().resolve(taskDefiniton.getTaskId());
  }

  private boolean writeS3MetadataFile(boolean finished) {
    Path logrotateDirectory = taskDefiniton.getServiceLogOut().getParent().resolve(configuration.getLogrotateToDirectory());
    
    S3UploadMetadata s3UploadMetadata = new S3UploadMetadata(logrotateDirectory.toString(), getS3Glob(), configuration.getS3Bucket(), getS3KeyPattern(), finished);
    
    String s3UploadMetadatafilename = String.format("%s%s", taskDefiniton.getTaskId(), configuration.getS3MetadataSuffix());
    
    Path s3UploadMetadataPath = configuration.getS3MetadataDirectory().resolve(s3UploadMetadatafilename);
    
    return writeObject(s3UploadMetadata, s3UploadMetadataPath);
  }

  private boolean writeObject(Object o, Path path) {
    return executorUtils.writeObject(o, path, log);
  }
  
}
