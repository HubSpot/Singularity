package com.hubspot.singularity.executor.task;

import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Joiner;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.hubspot.singularity.SingularityS3FormatHelper;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.models.LogrotateTemplateContext;
import com.hubspot.singularity.runner.base.shared.JsonObjectFileHelper;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;
import com.hubspot.singularity.runner.base.shared.TailMetadata;

public class SingularityExecutorTaskLogManager {

  private final SingularityExecutorTaskDefinition taskDefinition;
  private final TemplateManager templateManager;
  private final SingularityExecutorConfiguration configuration;
  private final Logger log;
  private final JsonObjectFileHelper jsonObjectFileHelper;

  public SingularityExecutorTaskLogManager(SingularityExecutorTaskDefinition taskDefinition, TemplateManager templateManager, SingularityExecutorConfiguration configuration, Logger log, JsonObjectFileHelper jsonObjectFileHelper) {
    this.log = log;
    this.taskDefinition = taskDefinition;
    this.templateManager = templateManager;
    this.configuration = configuration;
    this.jsonObjectFileHelper = jsonObjectFileHelper;
  }

  public void setup() {
    ensureServiceOutExists();
    writeLogrotateFile();
    writeTailMetadata(false);
    writeS3MetadataFile(false);
  }

  private void writeLogrotateFile() {
    log.info("Writing logrotate configuration file to {}", getLogrotateConfPath());
    templateManager.writeLogrotateFile(getLogrotateConfPath(), new LogrotateTemplateContext(configuration, taskDefinition));
  }

  public boolean teardown() {
    boolean writeTailMetadataSuccess = writeTailMetadata(true);

    ensureServiceOutExists();
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

    final Path tailOfLogPath = taskDefinition.getTaskDirectoryPath().resolve(configuration.getServiceFinishedTailLog());

    if (Files.exists(tailOfLogPath)) {
      log.debug("{} already existed, skipping tail", tailOfLogPath);
      return;
    }

    final List<String> cmd = ImmutableList.of(
        "tail",
        "-n",
        Integer.toString(configuration.getTailLogLinesToSave()),
        taskDefinition.getServiceLogOut());

    try {
      new SimpleProcessManager(log).runCommand(cmd, Redirect.to(tailOfLogPath.toFile()));
    } catch (Throwable t) {
      log.error("Failed saving tail of log {} to {}", taskDefinition.getServiceLogOut(), configuration.getServiceFinishedTailLog(), t);
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

  /**
   * Trigger manual logrotate run.
   * @return True on successful run or skip. False on error.
   */
  public boolean manualLogrotate() {
    if (!Files.exists(getLogrotateConfPath())) {
      log.info("{} did not exist, skipping manual logrotation", getLogrotateConfPath());
      return true;
    }

    final List<String> command = ImmutableList.of(
        configuration.getLogrotateCommand(),
        "-f",
        "-s",
        taskDefinition.getLogrotateStateFilePath().toString(),
        getLogrotateConfPath().toString());

    try {
      new SimpleProcessManager(log).runCommand(command);
      return true;
    } catch (Throwable t) {
      log.warn("Tried to manually logrotate using {}, but caught", getLogrotateConfPath(), t);
      return false;
    }
  }

  private void ensureServiceOutExists() {
    try {
      if (!Files.exists(taskDefinition.getServiceLogOutPath())) {
        Files.createFile(taskDefinition.getServiceLogOutPath());
      }
    } catch (FileAlreadyExistsException faee) {
      log.debug("Executor out {} already existed", taskDefinition.getServiceLogOut());
    } catch (Throwable t) {
      log.error("Failed creating executor out {}", taskDefinition.getServiceLogOut(), t);
    }
  }

  private boolean writeTailMetadata(boolean finished) {
    if (!taskDefinition.getExecutorData().getLoggingTag().isPresent()) {
      if (!finished) {
        log.warn("Not writing logging metadata because logging tag is absent");
      }
      return true;
    }

    final TailMetadata tailMetadata = new TailMetadata(taskDefinition.getServiceLogOut(), taskDefinition.getExecutorData().getLoggingTag().get(), taskDefinition.getExecutorData().getLoggingExtraFields(), finished);
    final Path path = TailMetadata.getTailMetadataPath(configuration.getLogMetadataDirectory(), configuration.getLogMetadataSuffix(), tailMetadata);

    return jsonObjectFileHelper.writeObject(tailMetadata, path, log);
  }

  /**
   * Return a String for generating a PathMatcher.
   * The matching files are caught by the S3 Uploader and pushed to S3.
   * @return file glob String.
   */
  private String getS3Glob() {
    List<String> fileNames = new LinkedList<>();
    fileNames.add(taskDefinition.getServiceLogOutPath().getFileName().toString());
    fileNames.addAll(Arrays.asList(configuration.getS3FilesToBackup()));

    return String.format("{%s}*.gz*", Joiner.on(",").join(fileNames));
  }

  private String getS3KeyPattern() {
    String s3KeyPattern = configuration.getS3KeyPattern();

    final SingularityTaskId singularityTaskId = getSingularityTaskId();

    return SingularityS3FormatHelper.getS3KeyFormat(s3KeyPattern, singularityTaskId, taskDefinition.getExecutorData().getLoggingTag());
  }

  private SingularityTaskId getSingularityTaskId() {
    return SingularityTaskId.valueOf(taskDefinition.getTaskId());
  }

  public Path getLogrotateConfPath() {
    return configuration.getLogrotateConfDirectory().resolve(taskDefinition.getTaskId());
  }

  private boolean writeS3MetadataFile(boolean finished) {
    Path logrotateDirectory = taskDefinition.getServiceLogOutPath().getParent().resolve(configuration.getLogrotateToDirectory());

    S3UploadMetadata s3UploadMetadata = new S3UploadMetadata(logrotateDirectory.toString(), getS3Glob(), configuration.getS3Bucket(), getS3KeyPattern(), finished);

    String s3UploadMetadataFileName = String.format("%s%s", taskDefinition.getTaskId(), configuration.getS3MetadataSuffix());

    Path s3UploadMetadataPath = configuration.getS3MetadataDirectory().resolve(s3UploadMetadataFileName);

    return jsonObjectFileHelper.writeObject(s3UploadMetadata, s3UploadMetadataPath, log);
  }

}
