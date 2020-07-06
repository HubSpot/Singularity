package com.hubspot.singularity.executor.task;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hubspot.singularity.SingularityS3FormatHelper;
import com.hubspot.singularity.SingularityS3UploaderFile;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.config.SingularityExecutorLogrotateAdditionalFile;
import com.hubspot.singularity.executor.models.LogrotateCronTemplateContext;
import com.hubspot.singularity.executor.models.LogrotateTemplateContext;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseConfiguration;
import com.hubspot.singularity.runner.base.shared.JsonObjectFileHelper;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;
import com.hubspot.singularity.runner.base.shared.TailMetadata;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

public class SingularityExecutorTaskLogManager {
  private final SingularityExecutorTaskDefinition taskDefinition;
  private final TemplateManager templateManager;
  private final SingularityRunnerBaseConfiguration baseConfiguration;
  private final SingularityExecutorConfiguration configuration;
  private final Logger log;
  private final JsonObjectFileHelper jsonObjectFileHelper;
  private final SingularityExecutorLogrotateFrequency logrotateFrequency;
  private final ScheduledExecutorService logCheckExecutor;

  private Future<?> logCheckFuture = null;

  public SingularityExecutorTaskLogManager(
    SingularityExecutorTaskDefinition taskDefinition,
    TemplateManager templateManager,
    SingularityRunnerBaseConfiguration baseConfiguration,
    SingularityExecutorConfiguration configuration,
    Logger log,
    JsonObjectFileHelper jsonObjectFileHelper,
    boolean startServiceLogChecker
  ) {
    this.log = log;
    this.taskDefinition = taskDefinition;
    this.templateManager = templateManager;
    this.configuration = configuration;
    this.baseConfiguration = baseConfiguration;
    this.jsonObjectFileHelper = jsonObjectFileHelper;
    this.logrotateFrequency =
      taskDefinition
        .getExecutorData()
        .getLogrotateFrequency()
        .orElse(configuration.getLogrotateFrequency());
    if (startServiceLogChecker) {
      this.logCheckExecutor =
        Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setNameFormat("service-log-checker-%d").build()
        );
    } else {
      logCheckExecutor = null;
    }
  }

  public void setup() {
    ensureServiceOutExists();
    writeLogrotateFile();
    writeTailMetadata(false);
    writeS3MetadataFileForRotatedFiles(false);
    startLogChecker();
  }

  private void startLogChecker() {
    try {
      if (logCheckExecutor != null) {
        log.info(
          "Starting service log checker to rotate logs over {} MB",
          configuration.getMaxServiceLogSizeMb()
        );
        logCheckFuture =
          logCheckExecutor.scheduleAtFixedRate(
            this::checkServiceLogSize,
            5,
            5,
            TimeUnit.MINUTES
          );
      }
    } catch (Throwable t) {
      log.warn("Could not start service log checker", t);
    }
  }

  private void stopLogChecker() {
    try {
      if (logCheckFuture != null) {
        logCheckFuture.cancel(true);
      }
      if (logCheckExecutor != null) {
        logCheckExecutor.shutdown();
      }
    } catch (Throwable t) {
      log.warn("Coud not properly shut down log checker", t);
    }
  }

  private void checkServiceLogSize() {
    try {
      long fileBytes = taskDefinition.getServiceLogOutPath().toFile().length();
      long fileMb = fileBytes / 1024 / 1024;
      log.debug(
        "service log is currently {} MB (limit before logrotate: {} MB)",
        fileMb,
        configuration.getMaxServiceLogSizeMb()
      );
      if (
        configuration.getMaxServiceLogSizeMb().isPresent() &&
        fileMb > configuration.getMaxServiceLogSizeMb().get()
      ) {
        manualLogrotate();
      }
    } catch (Throwable t) {
      log.warn("Could not run file size check on service log", t);
    }
  }

  @SuppressFBWarnings
  private boolean writeS3MetadataFileForRotatedFiles(boolean finished) {
    final Path serviceLogOutPath = taskDefinition.getServiceLogOutPath();
    final Path serviceLogParent = serviceLogOutPath.getParent();
    final Path logrotateDirectory = serviceLogParent.resolve(
      configuration.getLogrotateToDirectory()
    );

    List<String> handledLogs = new ArrayList<>();
    int index = 1;
    boolean result = true;

    for (SingularityS3UploaderFile additionalFile : taskDefinition
      .getExecutorData()
      .getS3UploaderAdditionalFiles()) {
      Path directory = additionalFile.getDirectory().isPresent()
        ? taskDefinition
          .getTaskDirectoryPath()
          .resolve(additionalFile.getDirectory().get())
        : taskDefinition.getTaskDirectoryPath();

      if (!directory.toAbsolutePath().startsWith(taskDefinition.getTaskDirectoryPath())) {
        log.warn(
          "Received request to upload files in directory outside task sandbox; these will not be uploaded ({})",
          directory
        );
        continue;
      }

      String fileGlob = additionalFile.getFilename() != null &&
        additionalFile.getFilename().contains("*")
        ? additionalFile.getFilename()
        : String.format("%s*.[gb]z*", additionalFile.getFilename());
      result =
        result &&
        writeS3MetadataFile(
          additionalFile
            .getS3UploaderFilenameHint()
            .orElse(String.format("file%d", index)),
          directory,
          fileGlob,
          additionalFile.getS3UploaderBucket(),
          additionalFile.getS3UploaderKeyPattern(),
          finished,
          additionalFile.getS3StorageClass().isPresent()
            ? additionalFile.getS3StorageClass()
            : taskDefinition.getExecutorData().getS3StorageClass(),
          additionalFile.getApplyS3StorageClassAfterBytes().isPresent()
            ? additionalFile.getApplyS3StorageClassAfterBytes()
            : taskDefinition.getExecutorData().getApplyS3StorageClassAfterBytes(),
          additionalFile.isCheckSubdirectories()
        );
      index++;
      handledLogs.add(additionalFile.getFilename());
    }

    // Allow an additional file to override the upload settings for service.log
    if (!handledLogs.contains(taskDefinition.getServiceLogFileName())) {
      result =
        result &&
        writeS3MetadataFile(
          "default",
          logrotateDirectory,
          String.format(
            "%s*.[gb]z*",
            taskDefinition.getServiceLogOutPath().getFileName()
          ),
          Optional.empty(),
          Optional.empty(),
          finished,
          taskDefinition.getExecutorData().getS3StorageClass(),
          taskDefinition.getExecutorData().getApplyS3StorageClassAfterBytes(),
          false
        );
    }

    return result;
  }

  private void writeLogrotateFile() {
    log.info(
      "Writing non-hourly logrotate configuration file to {}",
      getLogrotateConfPath()
    );
    templateManager.writeLogrotateFile(
      getLogrotateConfPath(),
      new LogrotateTemplateContext(configuration, taskDefinition)
    );

    // Get the frequency and cron schedule for an additional file with an HOURLY schedule, if there is any
    Optional<SingularityExecutorLogrotateFrequency> additionalFileFrequency = getAdditionalHourlyFileFrequency();

    // if any additional file or the global setting has an hourly rotation,
    // or if any additional file has a size-based rotation threshold,
    // write a separate rotate config and force rotate using a cron schedule
    boolean needsHourlyCronWithForceLogrotate =
      additionalFileFrequency.isPresent() &&
      additionalFileFrequency.get().getCronSchedule().isPresent();

    boolean globalLogrotateRequiresCronWithForceLogrotate = logrotateFrequency
      .getCronSchedule()
      .isPresent();

    boolean needsHourlyCronWithNonForcedLogrotate = requiresSizeBasedRotation();

    if (
      needsHourlyCronWithForceLogrotate ||
      globalLogrotateRequiresCronWithForceLogrotate ||
      needsHourlyCronWithNonForcedLogrotate
    ) {
      File hourlyLogrotateDir = new File(configuration.getLogrotateHourlyConfDirectory());
      if (!hourlyLogrotateDir.exists()) {
        if (!hourlyLogrotateDir.mkdir()) {
          log.warn(
            "Could not create hourly logrotate directory at {}",
            configuration.getLogrotateHourlyConfDirectory()
          );
        }
      }
    }

    if (needsHourlyCronWithForceLogrotate) {
      log.info(
        "Writing hourly logrotate configuration file to {}",
        getLogrotateHourlyConfPath()
      );
      templateManager.writeHourlyLogrotateFile(
        getLogrotateHourlyConfPath(),
        new LogrotateTemplateContext(configuration, taskDefinition)
      );
    }

    if (requiresSizeBasedRotation()) {
      log.info(
        "Writing size-based logrotate configuration file to {}",
        getLogrotateSizeBasedConfPath()
      );
      templateManager.writeSizeBasedLogrotateFile(
        getLogrotateSizeBasedConfPath(),
        new LogrotateTemplateContext(configuration, taskDefinition)
      );
    }

    if (
      needsHourlyCronWithForceLogrotate ||
      globalLogrotateRequiresCronWithForceLogrotate ||
      needsHourlyCronWithNonForcedLogrotate
    ) {
      String cronScheduleString = SingularityExecutorLogrotateFrequency
        .HOURLY.getCronSchedule()
        .get();

      log.info(
        "Writing logrotate cron entry with schedule '{}' to {}",
        cronScheduleString,
        getLogrotateCronPath()
      );
      templateManager.writeCronEntryForLogrotate(
        getLogrotateCronPath(),
        new LogrotateCronTemplateContext(
          configuration,
          taskDefinition,
          // By running logrotate hourly via cron, we cover all HOURLY logrotate configs which require `-f`,
          // as well as all size-based configs which do not
          SingularityExecutorLogrotateFrequency.HOURLY
        )
      );
    }
  }

  /**
   *
   * @return Frequency (and contained cron schedule) of the hourly rotation additional file
   */
  private Optional<SingularityExecutorLogrotateFrequency> getAdditionalHourlyFileFrequency() {
    for (SingularityExecutorLogrotateAdditionalFile file : configuration.getLogrotateAdditionalFiles()) {
      if (
        file.getLogrotateFrequencyOverride().isPresent() &&
        file
          .getLogrotateFrequencyOverride()
          .get()
          .equals(SingularityExecutorLogrotateFrequency.HOURLY) &&
        file.getLogrotateFrequencyOverride().get().getCronSchedule().isPresent()
      ) {
        return Optional.of(file.getLogrotateFrequencyOverride().get());
      }
    }
    return Optional.empty();
  }

  private boolean requiresSizeBasedRotation() {
    return configuration
      .getLogrotateAdditionalFiles()
      .stream()
      .anyMatch(
        logrotateAdditionalFile ->
          logrotateAdditionalFile.getLogrotateSizeOverride().isPresent() &&
          !logrotateAdditionalFile.getLogrotateSizeOverride().get().isEmpty()
      );
  }

  @SuppressFBWarnings
  public boolean teardown() {
    stopLogChecker();
    boolean writeTailMetadataSuccess = writeTailMetadata(true);

    ensureServiceOutExists();

    if (taskDefinition.shouldLogrotateLogFile()) {
      copyLogTail();
    }

    boolean writeS3MetadataForNonLogRotatedFileSuccess = true;

    if (!taskDefinition.shouldLogrotateLogFile()) {
      writeS3MetadataForNonLogRotatedFileSuccess =
        writeS3MetadataFile(
          "unrotated",
          taskDefinition.getServiceLogOutPath().getParent(),
          taskDefinition.getServiceLogOutPath().getFileName().toString(),
          Optional.<String>empty(),
          Optional.<String>empty(),
          true,
          taskDefinition.getExecutorData().getS3StorageClass(),
          taskDefinition.getExecutorData().getApplyS3StorageClassAfterBytes(),
          false
        );
    }

    if (manualLogrotate()) {
      boolean removeLogRotateFileSuccess = removeLogrotateFile();

      removeEmptyServiceOut();

      boolean writeS3MetadataForLogrotatedFilesSuccess = writeS3MetadataFileForRotatedFiles(
        true
      );

      return (
        writeTailMetadataSuccess &&
        removeLogRotateFileSuccess &&
        writeS3MetadataForLogrotatedFilesSuccess &&
        writeS3MetadataForNonLogRotatedFileSuccess
      );
    } else {
      return removeLogrotateFile();
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
      taskDefinition.getServiceLogOut()
    );

    try {
      new SimpleProcessManager(log).runCommand(cmd, Redirect.to(tailOfLogPath.toFile()));
    } catch (Throwable t) {
      log.error(
        "Failed saving tail of log {} to {}",
        taskDefinition.getServiceLogOut(),
        taskDefinition.getServiceFinishedTailLogPath(),
        t
      );
    }
  }

  public boolean removeLogrotateFile() {
    boolean deleted = false;
    try {
      if (Files.exists(getLogrotateConfPath())) {
        deleted = Files.deleteIfExists(getLogrotateConfPath());
        log.debug("Deleted {} : {}", getLogrotateConfPath(), deleted);
      } else {
        deleted = true;
      }
    } catch (Throwable t) {
      log.debug("Couldn't delete {}", getLogrotateConfPath(), t);
      return false;
    }

    Optional<SingularityExecutorLogrotateFrequency> additionalFileFreq = getAdditionalHourlyFileFrequency();
    try {
      if (
        (
          additionalFileFreq.isPresent() &&
          additionalFileFreq.get().getCronSchedule().isPresent()
        ) ||
        logrotateFrequency.getCronSchedule().isPresent()
      ) {
        boolean hourlyConfDeleted =
          !Files.exists(getLogrotateHourlyConfPath()) ||
          Files.deleteIfExists(getLogrotateHourlyConfPath());
        log.debug("Deleted {} : {}", getLogrotateHourlyConfPath(), hourlyConfDeleted);
        deleted = deleted && hourlyConfDeleted;
      }
    } catch (Throwable t) {
      log.debug("Couldn't delete {}", getLogrotateHourlyConfPath(), t);
      return false;
    }

    try {
      if (requiresSizeBasedRotation()) {
        boolean sizeBasedConfDeleted =
          !Files.exists(getLogrotateSizeBasedConfPath()) ||
          Files.deleteIfExists(getLogrotateSizeBasedConfPath());
        log.debug(
          "Deleted {} : {}",
          getLogrotateSizeBasedConfPath(),
          sizeBasedConfDeleted
        );
        deleted = deleted && sizeBasedConfDeleted;
      }
    } catch (Throwable t) {
      log.debug("Couldn't delete {}", getLogrotateSizeBasedConfPath(), t);
      return false;
    }

    try {
      if (
        (
          additionalFileFreq.isPresent() &&
          additionalFileFreq.get().getCronSchedule().isPresent()
        ) ||
        logrotateFrequency.getCronSchedule().isPresent()
      ) {
        boolean cronDeleted =
          !Files.exists(getLogrotateCronPath()) ||
          Files.deleteIfExists(getLogrotateCronPath());
        log.debug("Deleted {} : {}", getLogrotateCronPath(), cronDeleted);
        deleted = deleted && cronDeleted;
      }
    } catch (Throwable t) {
      log.debug("Couldn't delete {}", getLogrotateCronPath(), t);
      return false;
    }

    return deleted;
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
      getLogrotateConfPath().toString()
    );

    try {
      new SimpleProcessManager(log).runCommand(command);
      return true;
    } catch (Throwable t) {
      log.warn(
        "Tried to manually logrotate using {}, but caught",
        getLogrotateConfPath(),
        t
      );
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
      if (
        Files.exists(taskDefinition.getServiceLogOutPath()) &&
        Files.size(taskDefinition.getServiceLogOutPath()) == 0
      ) {
        Files.deleteIfExists(taskDefinition.getServiceLogOutPath());
      }
    } catch (Throwable t) {
      log.error(
        "Failed checking/deleting executor out {}",
        taskDefinition.getServiceLogOut(),
        t
      );
    }
  }

  private boolean writeTailMetadata(boolean finished) {
    if (!taskDefinition.getExecutorData().getLoggingTag().isPresent()) {
      if (!finished) {
        log.warn("Not writing logging metadata because logging tag is absent");
      }
      return true;
    }

    final TailMetadata tailMetadata = new TailMetadata(
      taskDefinition.getServiceLogOut(),
      taskDefinition.getExecutorData().getLoggingTag().get(),
      taskDefinition.getExecutorData().getLoggingExtraFields(),
      finished
    );
    final Path path = TailMetadata.getTailMetadataPath(
      Paths.get(baseConfiguration.getLogWatcherMetadataDirectory()),
      baseConfiguration.getLogWatcherMetadataSuffix(),
      tailMetadata
    );

    return jsonObjectFileHelper.writeObject(tailMetadata, path, log);
  }

  private String getS3KeyPattern(String s3KeyPattern) {
    final SingularityTaskId singularityTaskId = getSingularityTaskId();

    return SingularityS3FormatHelper.getS3KeyFormat(
      s3KeyPattern,
      singularityTaskId,
      taskDefinition.getExecutorData().getLoggingTag(),
      taskDefinition
        .getExecutorData()
        .getRequestGroup()
        .orElse(SingularityS3FormatHelper.DEFAULT_GROUP_NAME)
    );
  }

  private SingularityTaskId getSingularityTaskId() {
    return SingularityTaskId.valueOf(taskDefinition.getTaskId());
  }

  public Path getLogrotateConfPath() {
    return Paths
      .get(configuration.getLogrotateConfDirectory())
      .resolve(taskDefinition.getTaskId());
  }

  public Path getLogrotateHourlyConfPath() {
    return Paths
      .get(configuration.getLogrotateHourlyConfDirectory())
      .resolve(taskDefinition.getTaskId());
  }

  public Path getLogrotateSizeBasedConfPath() {
    return Paths
      .get(configuration.getLogrotateHourlyConfDirectory())
      .resolve(taskDefinition.getTaskId() + ".sizebased");
  }

  public Path getLogrotateCronPath() {
    return Paths
      .get(configuration.getCronDirectory())
      .resolve(taskDefinition.getTaskId() + ".logrotate");
  }

  private boolean writeS3MetadataFile(
    String filenameHint,
    Path pathToS3Directory,
    String globForS3Files,
    Optional<String> s3Bucket,
    Optional<String> s3KeyPattern,
    boolean finished,
    Optional<String> s3StorageClass,
    Optional<Long> applyS3StorageClassAfterBytes,
    boolean checkSubdirectories
  ) {
    final String s3UploaderBucket = s3Bucket.orElse(
      taskDefinition.getExecutorData().getDefaultS3Bucket()
    );

    if (Strings.isNullOrEmpty(s3UploaderBucket)) {
      log.warn(
        "No s3 bucket specified, not writing s3 metadata for file matcher {}",
        globForS3Files
      );
      return false;
    }

    S3UploadMetadata s3UploadMetadata = new S3UploadMetadata(
      pathToS3Directory.toString(),
      globForS3Files,
      s3UploaderBucket,
      getS3KeyPattern(
        s3KeyPattern.orElse(taskDefinition.getExecutorData().getS3UploaderKeyPattern())
      ),
      finished,
      Optional.<String>empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      s3StorageClass,
      applyS3StorageClassAfterBytes,
      Optional.of(finished),
      Optional.of(checkSubdirectories),
      Optional.empty(),
      Collections.emptyMap(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()
    );

    String s3UploadMetadataFileName = String.format(
      "%s-%s%s",
      taskDefinition.getTaskId(),
      filenameHint,
      baseConfiguration.getS3UploaderMetadataSuffix()
    );

    Path s3UploadMetadataPath = Paths
      .get(baseConfiguration.getS3UploaderMetadataDirectory())
      .resolve(s3UploadMetadataFileName);

    return jsonObjectFileHelper.writeObject(s3UploadMetadata, s3UploadMetadataPath, log);
  }
}
