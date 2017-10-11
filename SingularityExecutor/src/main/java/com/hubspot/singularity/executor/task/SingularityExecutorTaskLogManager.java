package com.hubspot.singularity.executor.task;

import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.hubspot.singularity.SingularityS3FormatHelper;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.SingularityS3UploaderFile;
import com.hubspot.singularity.executor.models.LogrotateCronTemplateContext;
import com.hubspot.singularity.executor.models.LogrotateTemplateContext;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseConfiguration;
import com.hubspot.singularity.runner.base.shared.JsonObjectFileHelper;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;
import com.hubspot.singularity.runner.base.shared.TailMetadata;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class SingularityExecutorTaskLogManager {

  private final SingularityExecutorTaskDefinition taskDefinition;
  private final TemplateManager templateManager;
  private final SingularityRunnerBaseConfiguration baseConfiguration;
  private final SingularityExecutorConfiguration configuration;
  private final Logger log;
  private final JsonObjectFileHelper jsonObjectFileHelper;
  private final SingularityExecutorLogrotateFrequency logrotateFrequency;

  public SingularityExecutorTaskLogManager(SingularityExecutorTaskDefinition taskDefinition, TemplateManager templateManager, SingularityRunnerBaseConfiguration baseConfiguration, SingularityExecutorConfiguration configuration, Logger log, JsonObjectFileHelper jsonObjectFileHelper) {
    this.log = log;
    this.taskDefinition = taskDefinition;
    this.templateManager = templateManager;
    this.configuration = configuration;
    this.baseConfiguration = baseConfiguration;
    this.jsonObjectFileHelper = jsonObjectFileHelper;
    this.logrotateFrequency = taskDefinition.getExecutorData().getLogrotateFrequency().or(configuration.getLogrotateFrequency());
  }

  public void setup() {
    ensureServiceOutExists();
    writeLogrotateFile();
    writeTailMetadata(false);
    writeS3MetadataFileForRotatedFiles(false);
  }

  @SuppressFBWarnings
  private boolean writeS3MetadataFileForRotatedFiles(boolean finished) {
    final Path serviceLogOutPath = taskDefinition.getServiceLogOutPath();
    final Path serviceLogParent = serviceLogOutPath.getParent();
    final Path logrotateDirectory = serviceLogParent.resolve(configuration.getLogrotateToDirectory());

    List<String> handledLogs = new ArrayList<>();
    int index = 1;
    boolean result = true;

    for (SingularityS3UploaderFile additionalFile : taskDefinition.getExecutorData().getS3UploaderAdditionalFiles()) {
      Path directory = additionalFile.getDirectory().isPresent() ? taskDefinition.getTaskDirectoryPath().resolve(additionalFile.getDirectory().get()) : taskDefinition.getTaskDirectoryPath();
      String fileGlob = additionalFile.getFilename() != null && additionalFile.getFilename().contains("*") ? additionalFile.getFilename() : String.format("%s*.[gb]z*", additionalFile.getFilename());
      result = result && writeS3MetadataFile(additionalFile.getS3UploaderFilenameHint().or(String.format("file%d", index)), directory, fileGlob, additionalFile.getS3UploaderBucket(), additionalFile.getS3UploaderKeyPattern(), finished,
          additionalFile.getS3StorageClass().or(taskDefinition.getExecutorData().getS3StorageClass()), additionalFile.getApplyS3StorageClassAfterBytes().or(taskDefinition.getExecutorData().getApplyS3StorageClassAfterBytes()),
          additionalFile.isCheckSubdirectories());
      index++;
      handledLogs.add(additionalFile.getFilename());
    }

    // Allow an additional file to override the upload settings for service.log
    if (!handledLogs.contains(taskDefinition.getServiceLogFileName())) {
      result = result && writeS3MetadataFile("default", logrotateDirectory, String.format("%s*.[gb]z*", taskDefinition.getServiceLogOutPath().getFileName()), Optional.absent(), Optional.absent(), finished,
         taskDefinition.getExecutorData().getS3StorageClass(), taskDefinition.getExecutorData().getApplyS3StorageClassAfterBytes(), false);
    }

    return result;
  }

  private void writeLogrotateFile() {
    log.info("Writing logrotate configuration file to {}", getLogrotateConfPath());
    templateManager.writeLogrotateFile(getLogrotateConfPath(), new LogrotateTemplateContext(configuration, taskDefinition));

    if (logrotateFrequency.getCronSchedule().isPresent()) {
      log.info("Writing logrotate cron entry with schedule '{}' to {}", logrotateFrequency.getCronSchedule().get(), getLogrotateCronPath());
      templateManager.writeCronEntryForLogrotate(getLogrotateCronPath(), new LogrotateCronTemplateContext(configuration, taskDefinition, logrotateFrequency));
    }
  }

  @SuppressFBWarnings
  public boolean teardown() {
    boolean writeTailMetadataSuccess = writeTailMetadata(true);

    ensureServiceOutExists();

    if (taskDefinition.shouldLogrotateLogFile()) {
      copyLogTail();
    }

    boolean writeS3MetadataForNonLogRotatedFileSuccess = true;

    if (!taskDefinition.shouldLogrotateLogFile()) {
      writeS3MetadataForNonLogRotatedFileSuccess = writeS3MetadataFile("unrotated", taskDefinition.getServiceLogOutPath().getParent(),
          taskDefinition.getServiceLogOutPath().getFileName().toString(), Optional.<String>absent(), Optional.<String>absent(), true,
          taskDefinition.getExecutorData().getS3StorageClass(), taskDefinition.getExecutorData().getApplyS3StorageClassAfterBytes(),
          false);
    }

    if (manualLogrotate()) {
      boolean removeLogRotateFileSuccess = removeLogrotateFile();

      removeEmptyServiceOut();

      boolean writeS3MetadataForLogrotatedFilesSuccess = writeS3MetadataFileForRotatedFiles(true);

      return writeTailMetadataSuccess && removeLogRotateFileSuccess && writeS3MetadataForLogrotatedFilesSuccess && writeS3MetadataForNonLogRotatedFileSuccess;
    } else {
      return false;
    }
  }

  private void copyLogTail() {
    if (configuration.getTailLogLinesToSave() <= 0) {
      return;
    }

    final Path tailOfLogPath = taskDefinition.getServiceFinishedTailLogPath();

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
      log.error("Failed saving tail of log {} to {}", taskDefinition.getServiceLogOut(), taskDefinition.getServiceFinishedTailLogPath(), t);
    }
  }

  public boolean removeLogrotateFile() {
    boolean deleted = false;
    try {
      deleted = Files.deleteIfExists(getLogrotateConfPath());
      if (logrotateFrequency.getCronSchedule().isPresent()) {
        boolean cronDeleted = Files.deleteIfExists(getLogrotateCronPath());
        deleted = deleted || cronDeleted;
      }
    } catch (Throwable t) {
      log.trace("Couldn't delete {}", getLogrotateConfPath(), t);
      return false;
    }
    log.trace("Deleted {} : {}", getLogrotateConfPath(), deleted);
    return true;
  }

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

  private void removeEmptyServiceOut() {
    try {
      if (Files.exists(taskDefinition.getServiceLogOutPath()) && Files.size(taskDefinition.getServiceLogOutPath()) == 0) {
        Files.deleteIfExists(taskDefinition.getServiceLogOutPath());
      }
    } catch (Throwable t) {
      log.error("Failed checking/deleting executor out {}", taskDefinition.getServiceLogOut(), t);
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
    final Path path = TailMetadata.getTailMetadataPath(Paths.get(baseConfiguration.getLogWatcherMetadataDirectory()), baseConfiguration.getLogWatcherMetadataSuffix(), tailMetadata);

    return jsonObjectFileHelper.writeObject(tailMetadata, path, log);
  }

  private String getS3KeyPattern(String s3KeyPattern) {
    final SingularityTaskId singularityTaskId = getSingularityTaskId();

    return SingularityS3FormatHelper.getS3KeyFormat(s3KeyPattern, singularityTaskId, taskDefinition.getExecutorData().getLoggingTag(), taskDefinition.getExecutorData().getRequestGroup().or(SingularityS3FormatHelper.DEFAULT_GROUP_NAME));
  }

  private SingularityTaskId getSingularityTaskId() {
    return SingularityTaskId.valueOf(taskDefinition.getTaskId());
  }

  public Path getLogrotateConfPath() {
    return Paths.get(configuration.getLogrotateConfDirectory()).resolve(taskDefinition.getTaskId());
  }

  public Path getLogrotateCronPath() {
    return Paths.get(configuration.getCronDirectory()).resolve(taskDefinition.getTaskId() + ".logrotate");
  }

  private boolean writeS3MetadataFile(String filenameHint, Path pathToS3Directory, String globForS3Files, Optional<String> s3Bucket, Optional<String> s3KeyPattern, boolean finished,
    Optional<String> s3StorageClass, Optional<Long> applyS3StorageClassAfterBytes, boolean checkSubdirectories) {
    final String s3UploaderBucket = s3Bucket.or(taskDefinition.getExecutorData().getDefaultS3Bucket());

    if (Strings.isNullOrEmpty(s3UploaderBucket)) {
      log.warn("No s3 bucket specified, not writing s3 metadata for file matcher {}", globForS3Files);
      return false;
    }

    S3UploadMetadata s3UploadMetadata = new S3UploadMetadata(pathToS3Directory.toString(), globForS3Files, s3UploaderBucket, getS3KeyPattern(s3KeyPattern.or(taskDefinition.getExecutorData().getS3UploaderKeyPattern())), finished, Optional.<String> absent(),
        Optional. absent(), Optional. absent(), Optional. absent(), Optional. absent(), s3StorageClass, applyS3StorageClassAfterBytes, Optional.of(finished), Optional.of(checkSubdirectories));

    String s3UploadMetadataFileName = String.format("%s-%s%s", taskDefinition.getTaskId(), filenameHint, baseConfiguration.getS3UploaderMetadataSuffix());

    Path s3UploadMetadataPath = Paths.get(baseConfiguration.getS3UploaderMetadataDirectory()).resolve(s3UploadMetadataFileName);

    return jsonObjectFileHelper.writeObject(s3UploadMetadata, s3UploadMetadataPath, log);
  }

}
