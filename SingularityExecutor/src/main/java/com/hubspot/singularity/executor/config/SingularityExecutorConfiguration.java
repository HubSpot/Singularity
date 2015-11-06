package com.hubspot.singularity.executor.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;
import com.hubspot.singularity.runner.base.constraints.DirectoryExists;

@Configuration(filename = "/etc/singularity.executor.yaml", consolidatedField = "executor")
public class SingularityExecutorConfiguration extends BaseRunnerConfiguration {
  public static final String SHUTDOWN_TIMEOUT_MILLIS = "executor.shutdown.timeout.millis";

  public static final String HARD_KILL_AFTER_MILLIS = "executor.hard.kill.after.millis";
  public static final String NUM_CORE_KILL_THREADS = "executor.num.core.kill.threads";

  public static final String NUM_CORE_THREAD_CHECK_THREADS = "executor.num.core.thread.check.threads";
  public static final String CHECK_THREADS_EVERY_MILLIS = "executor.check.threads.every.millis";

  public static final String MAX_TASK_MESSAGE_LENGTH = "executor.status.update.max.task.message.length";

  public static final String IDLE_EXECUTOR_SHUTDOWN_AFTER_MILLIS = "executor.idle.shutdown.after.millis";
  public static final String SHUTDOWN_STOP_DRIVER_AFTER_MILLIS = "executor.shutdown.stop.driver.after.millis";

  public static final String TASK_APP_DIRECTORY = "executor.task.app.directory";

  public static final String TASK_EXECUTOR_JAVA_LOG_PATH = "executor.task.java.log.path";
  public static final String TASK_EXECUTOR_BASH_LOG_PATH = "executor.task.bash.log.path";
  public static final String TASK_SERVICE_LOG_PATH = "executor.task.service.log.path";

  public static final String DEFAULT_USER = "executor.default.user";

  public static final String GLOBAL_TASK_DEFINITION_DIRECTORY = "executor.global.task.definition.directory";
  public static final String GLOBAL_TASK_DEFINITION_SUFFIX = "executor.global.task.definition.suffix";

  public static final String LOGROTATE_COMMAND = "executor.logrotate.command";
  public static final String LOGROTATE_CONFIG_DIRECTORY = "executor.logrotate.config.folder";
  public static final String LOGROTATE_STATE_FILE = "executor.logrotate.state.file";
  public static final String LOGROTATE_DIRECTORY = "executor.logrotate.to.directory";
  public static final String LOGROTATE_MAXAGE_DAYS = "executor.logrotate.maxage.days";
  public static final String LOGROTATE_COUNT = "executor.logrotate.count";
  public static final String LOGROTATE_DATEFORMAT = "executor.logrotate.dateformat";

  public static final String LOGROTATE_EXTRAS_DATEFORMAT = "executor.logrotate.extras.dateformat";
  public static final String LOGROTATE_EXTRAS_FILES = "executor.logrotate.extras.files";

  public static final String TAIL_LOG_LINES_TO_SAVE = "executor.service.log.tail.lines.to.save";
  public static final String TAIL_LOG_FILENAME = "executor.service.log.tail.file.name";

  public static final String S3_FILES_TO_BACKUP = "executor.s3.uploader.extras.files";
  public static final String S3_UPLOADER_PATTERN = "executor.s3.uploader.pattern";
  public static final String S3_UPLOADER_BUCKET = "executor.s3.uploader.bucket";

  public static final String USE_LOCAL_DOWNLOAD_SERVICE = "executor.use.local.download.service";

  public static final String LOCAL_DOWNLOAD_SERVICE_TIMEOUT_MILLIS = "executor.local.download.service.timeout.millis";

  public static final String MAX_TASK_THREADS = "executor.max.task.threads";

  public static final String DOCKER_PREFIX = "executor.docker.prefix";
  public static final String DOCKER_STOP_TIMEOUT = "executor.docker.stop.timeout";

  @NotEmpty
  @JsonProperty
  private String executorJavaLog = "executor.java.log";

  @NotEmpty
  @JsonProperty
  private String executorBashLog = "executor.bash.log";

  @NotEmpty
  @JsonProperty
  private String serviceLog = "service.log";

  @NotEmpty
  @JsonProperty
  private String defaultRunAsUser;

  @NotEmpty
  @JsonProperty
  private String taskAppDirectory = "app";

  @Min(0)
  @JsonProperty
  private long shutdownTimeoutWaitMillis = TimeUnit.MINUTES.toMillis(5);

  @Min(0)
  @JsonProperty
  private long idleExecutorShutdownWaitMillis = TimeUnit.SECONDS.toMillis(30);

  @Min(0)
  @JsonProperty
  private long stopDriverAfterMillis = TimeUnit.SECONDS.toMillis(5);

  @NotEmpty
  @DirectoryExists
  @JsonProperty
  private String globalTaskDefinitionDirectory;

  @NotEmpty
  @JsonProperty
  private String globalTaskDefinitionSuffix = ".task.json";

  @Min(1)
  @JsonProperty
  private long hardKillAfterMillis = TimeUnit.MINUTES.toMillis(3);

  @Min(1)
  @JsonProperty
  private int killThreads = 1;

  @Min(1)
  @JsonProperty
  private int threadCheckThreads = 1;

  @Min(1)
  @JsonProperty
  private long checkThreadsEveryMillis = TimeUnit.SECONDS.toMillis(5);

  @Min(0)
  @JsonProperty
  private int maxTaskMessageLength = 80;

  @NotEmpty
  @JsonProperty
  private String logrotateCommand = "logrotate";

  @NotEmpty
  @JsonProperty
  private String logrotateStateFile = "logrotate.status";

  @NotEmpty
  @DirectoryExists
  @JsonProperty
  private String logrotateConfDirectory = "/etc/logrotate.d";

  @NotEmpty
  @JsonProperty
  private String logrotateToDirectory = "logs";

  @Min(1)
  @JsonProperty
  private int logrotateMaxageDays = 7;

  @Min(1)
  @JsonProperty
  private int logrotateCount = 20;

  @NotEmpty
  @JsonProperty
  private String logrotateDateformat= "-%Y%m%d%s";

  @NotEmpty
  @JsonProperty
  private String logrotateExtrasDateformat = "-%Y%m%d";

  @NotNull
  @JsonProperty
  private List<String> logrotateAdditionalFiles = Collections.emptyList();

  /**
   * Extra files to backup to S3 besides the service log.
   */
  @NotNull
  @JsonProperty
  private List<String> s3UploaderAdditionalFiles = Collections.emptyList();

  @Min(1)
  @JsonProperty
  private int tailLogLinesToSave = 2500;

  @NotEmpty
  @JsonProperty
  private String serviceFinishedTailLog = "tail_of_finished_service.log";

  @JsonProperty
  private String s3UploaderKeyPattern;

  @JsonProperty
  private String s3UploaderBucket;

  @JsonProperty
  private boolean useLocalDownloadService = false;

  @Min(1)
  @JsonProperty
  private long localDownloadServiceTimeoutMillis = TimeUnit.MINUTES.toMillis(3);

  @NotNull
  @JsonProperty
  private Optional<Integer> maxTaskThreads = Optional.absent();

  @JsonProperty
  private String dockerPrefix = "se-";

  @JsonProperty
  private int dockerStopTimeout = 15;

  @NotEmpty
  @JsonProperty
  private String cgroupsMesosCpuTasksFormat = "/cgroup/cpu/%s/tasks";

  @NotEmpty
  @JsonProperty
  private String procCgroupFormat = "/proc/%s/cgroup";

  @NotEmpty
  @JsonProperty
  private String switchUserCommandFormat = "sudo -E -u %s";

  @JsonProperty
  @NotEmpty
  private List<String> artifactSignatureVerificationCommand = Arrays.asList("/usr/bin/gpg", "--verify", "{artifactSignaturePath}");

  @JsonProperty
  private boolean failTaskOnInvalidArtifactSignature = true;

  @JsonProperty
  @NotEmpty
  private String signatureVerifyOut = "executor.gpg.out";

  public SingularityExecutorConfiguration() {
    super(Optional.of("singularity-executor.log"));
  }

  public List<String> getLogrotateAdditionalFiles() {
    return logrotateAdditionalFiles;
  }

  public void setLogrotateAdditionalFiles(List<String> logrotateAdditionalFiles) {
    this.logrotateAdditionalFiles = logrotateAdditionalFiles;
  }

  public List<String> getS3UploaderAdditionalFiles() {
    return s3UploaderAdditionalFiles;
  }

  public void setS3UploaderAdditionalFiles(List<String> s3UploaderAdditionalFiles) {
    this.s3UploaderAdditionalFiles = s3UploaderAdditionalFiles;
  }

  public String getExecutorJavaLog() {
    return executorJavaLog;
  }

  public String getExecutorBashLog() {
    return executorBashLog;
  }

  public String getServiceLog() {
    return serviceLog;
  }

  public String getDefaultRunAsUser() {
    return defaultRunAsUser;
  }

  public String getTaskAppDirectory() {
    return taskAppDirectory;
  }

  public long getShutdownTimeoutWaitMillis() {
    return shutdownTimeoutWaitMillis;
  }

  public long getIdleExecutorShutdownWaitMillis() {
    return idleExecutorShutdownWaitMillis;
  }

  public long getStopDriverAfterMillis() {
    return stopDriverAfterMillis;
  }

  public String getGlobalTaskDefinitionDirectory() {
    return globalTaskDefinitionDirectory;
  }

  public String getGlobalTaskDefinitionSuffix() {
    return globalTaskDefinitionSuffix;
  }

  public long getHardKillAfterMillis() {
    return hardKillAfterMillis;
  }

  public int getKillThreads() {
    return killThreads;
  }

  public int getThreadCheckThreads() {
    return threadCheckThreads;
  }

  public long getCheckThreadsEveryMillis() {
    return checkThreadsEveryMillis;
  }

  public int getMaxTaskMessageLength() {
    return maxTaskMessageLength;
  }

  public String getLogrotateCommand() {
    return logrotateCommand;
  }

  public String getLogrotateStateFile() {
    return logrotateStateFile;
  }

  public String getLogrotateConfDirectory() {
    return logrotateConfDirectory;
  }

  public String getLogrotateToDirectory() {
    return logrotateToDirectory;
  }

  public int getLogrotateMaxageDays() {
    return logrotateMaxageDays;
  }

  public int getLogrotateCount() {
    return logrotateCount;
  }

  public String getLogrotateDateformat() {
    return logrotateDateformat;
  }

  public String getLogrotateExtrasDateformat() {
    return logrotateExtrasDateformat;
  }

  public int getTailLogLinesToSave() {
    return tailLogLinesToSave;
  }

  public String getServiceFinishedTailLog() {
    return serviceFinishedTailLog;
  }

  public boolean isUseLocalDownloadService() {
    return useLocalDownloadService;
  }

  public long getLocalDownloadServiceTimeoutMillis() {
    return localDownloadServiceTimeoutMillis;
  }

  public Optional<Integer> getMaxTaskThreads() {
    return maxTaskThreads;
  }

  public String getDockerPrefix() {
    return dockerPrefix;
  }

  public int getDockerStopTimeout() {
    return dockerStopTimeout;
  }

  @JsonIgnore
  public Path getTaskDefinitionPath(String taskId) {
    return Paths.get(getGlobalTaskDefinitionDirectory()).resolve(MesosUtils.getSafeTaskIdForDirectory(taskId) + getGlobalTaskDefinitionSuffix());
  }

  public void setExecutorJavaLog(String executorJavaLog) {
    this.executorJavaLog = executorJavaLog;
  }

  public void setExecutorBashLog(String executorBashLog) {
    this.executorBashLog = executorBashLog;
  }

  public void setServiceLog(String serviceLog) {
    this.serviceLog = serviceLog;
  }

  public void setDefaultRunAsUser(String defaultRunAsUser) {
    this.defaultRunAsUser = defaultRunAsUser;
  }

  public void setTaskAppDirectory(String taskAppDirectory) {
    this.taskAppDirectory = taskAppDirectory;
  }

  public void setShutdownTimeoutWaitMillis(long shutdownTimeoutWaitMillis) {
    this.shutdownTimeoutWaitMillis = shutdownTimeoutWaitMillis;
  }

  public void setIdleExecutorShutdownWaitMillis(long idleExecutorShutdownWaitMillis) {
    this.idleExecutorShutdownWaitMillis = idleExecutorShutdownWaitMillis;
  }

  public void setStopDriverAfterMillis(long stopDriverAfterMillis) {
    this.stopDriverAfterMillis = stopDriverAfterMillis;
  }

  public void setGlobalTaskDefinitionDirectory(String globalTaskDefinitionDirectory) {
    this.globalTaskDefinitionDirectory = globalTaskDefinitionDirectory;
  }

  public void setGlobalTaskDefinitionSuffix(String globalTaskDefinitionSuffix) {
    this.globalTaskDefinitionSuffix = globalTaskDefinitionSuffix;
  }

  public void setHardKillAfterMillis(long hardKillAfterMillis) {
    this.hardKillAfterMillis = hardKillAfterMillis;
  }

  public void setKillThreads(int killThreads) {
    this.killThreads = killThreads;
  }

  public void setThreadCheckThreads(int threadCheckThreads) {
    this.threadCheckThreads = threadCheckThreads;
  }

  public void setCheckThreadsEveryMillis(long checkThreadsEveryMillis) {
    this.checkThreadsEveryMillis = checkThreadsEveryMillis;
  }

  public void setMaxTaskMessageLength(int maxTaskMessageLength) {
    this.maxTaskMessageLength = maxTaskMessageLength;
  }

  public void setLogrotateCommand(String logrotateCommand) {
    this.logrotateCommand = logrotateCommand;
  }

  public void setLogrotateStateFile(String logrotateStateFile) {
    this.logrotateStateFile = logrotateStateFile;
  }

  public void setLogrotateConfDirectory(String logrotateConfDirectory) {
    this.logrotateConfDirectory = logrotateConfDirectory;
  }

  public void setLogrotateToDirectory(String logrotateToDirectory) {
    this.logrotateToDirectory = logrotateToDirectory;
  }

  public void setLogrotateMaxageDays(int logrotateMaxageDays) {
    this.logrotateMaxageDays = logrotateMaxageDays;
  }

  public void setLogrotateCount(int logrotateCount) {
    this.logrotateCount = logrotateCount;
  }

  public void setLogrotateDateformat(String logrotateDateformat) {
    this.logrotateDateformat = logrotateDateformat;
  }

  public void setLogrotateExtrasDateformat(String logrotateExtrasDateformat) {
    this.logrotateExtrasDateformat = logrotateExtrasDateformat;
  }

  public void setTailLogLinesToSave(int tailLogLinesToSave) {
    this.tailLogLinesToSave = tailLogLinesToSave;
  }

  public void setServiceFinishedTailLog(String serviceFinishedTailLog) {
    this.serviceFinishedTailLog = serviceFinishedTailLog;
  }

  public String getS3UploaderKeyPattern() {
    return s3UploaderKeyPattern;
  }

  public void setS3UploaderKeyPattern(String s3UploaderKeyPattern) {
    this.s3UploaderKeyPattern = s3UploaderKeyPattern;
  }

  public String getS3UploaderBucket() {
    return s3UploaderBucket;
  }

  public void setS3UploaderBucket(String s3UploaderBucket) {
    this.s3UploaderBucket = s3UploaderBucket;
  }

  public void setUseLocalDownloadService(boolean useLocalDownloadService) {
    this.useLocalDownloadService = useLocalDownloadService;
  }

  public void setLocalDownloadServiceTimeoutMillis(long localDownloadServiceTimeoutMillis) {
    this.localDownloadServiceTimeoutMillis = localDownloadServiceTimeoutMillis;
  }

  public void setMaxTaskThreads(Optional<Integer> maxTaskThreads) {
    this.maxTaskThreads = maxTaskThreads;
  }

  public void setDockerPrefix(String dockerPrefix) {
    this.dockerPrefix = dockerPrefix;
  }

  public void setDockerStopTimeout(int dockerStopTimeout) {
    this.dockerStopTimeout = dockerStopTimeout;
  }


  public String getCgroupsMesosCpuTasksFormat() {
    return cgroupsMesosCpuTasksFormat;
  }

  public void setCgroupsMesosCpuTasksFormat(String cgroupsMesosCpuTasksFormat) {
    this.cgroupsMesosCpuTasksFormat = cgroupsMesosCpuTasksFormat;
  }

  public String getProcCgroupFormat() {
    return procCgroupFormat;
  }

  public void setProcCgroupFormat(String procCgroupFormat) {
    this.procCgroupFormat = procCgroupFormat;
  }

  public String getSwitchUserCommandFormat() {
    return switchUserCommandFormat;
  }

  public void setSwitchUserCommandFormat(String switchUserCommandFormat) {
    this.switchUserCommandFormat = switchUserCommandFormat;
  }

  public List<String> getArtifactSignatureVerificationCommand() {
    return artifactSignatureVerificationCommand;
  }

  public void setArtifactSignatureVerificationCommand(List<String> artifactSignatureVerificationCommand) {
    this.artifactSignatureVerificationCommand = artifactSignatureVerificationCommand;
  }

  public boolean isFailTaskOnInvalidArtifactSignature() {
    return failTaskOnInvalidArtifactSignature;
  }

  public void setFailTaskOnInvalidArtifactSignature(boolean failTaskOnInvalidArtifactSignature) {
    this.failTaskOnInvalidArtifactSignature = failTaskOnInvalidArtifactSignature;
  }

  public String getSignatureVerifyOut() {
    return signatureVerifyOut;
  }

  public void setSignatureVerifyOut(String signatureVerifyOut) {
    this.signatureVerifyOut = signatureVerifyOut;
  }

  @Override
  public String toString() {
    return "SingularityExecutorConfiguration[" +
            "executorJavaLog='" + executorJavaLog + '\'' +
            ", executorBashLog='" + executorBashLog + '\'' +
            ", serviceLog='" + serviceLog + '\'' +
            ", defaultRunAsUser='" + defaultRunAsUser + '\'' +
            ", taskAppDirectory='" + taskAppDirectory + '\'' +
            ", shutdownTimeoutWaitMillis=" + shutdownTimeoutWaitMillis +
            ", idleExecutorShutdownWaitMillis=" + idleExecutorShutdownWaitMillis +
            ", stopDriverAfterMillis=" + stopDriverAfterMillis +
            ", globalTaskDefinitionDirectory='" + globalTaskDefinitionDirectory + '\'' +
            ", globalTaskDefinitionSuffix='" + globalTaskDefinitionSuffix + '\'' +
            ", hardKillAfterMillis=" + hardKillAfterMillis +
            ", killThreads=" + killThreads +
            ", threadCheckThreads=" + threadCheckThreads +
            ", checkThreadsEveryMillis=" + checkThreadsEveryMillis +
            ", maxTaskMessageLength=" + maxTaskMessageLength +
            ", logrotateCommand='" + logrotateCommand + '\'' +
            ", logrotateStateFile='" + logrotateStateFile + '\'' +
            ", logrotateConfDirectory='" + logrotateConfDirectory + '\'' +
            ", logrotateToDirectory='" + logrotateToDirectory + '\'' +
            ", logrotateMaxageDays=" + logrotateMaxageDays +
            ", logrotateCount=" + logrotateCount +
            ", logrotateDateformat='" + logrotateDateformat + '\'' +
            ", logrotateExtrasDateformat='" + logrotateExtrasDateformat + '\'' +
            ", logrotateAdditionalFiles=" + logrotateAdditionalFiles +
            ", s3UploaderAdditionalFiles=" + s3UploaderAdditionalFiles +
            ", tailLogLinesToSave=" + tailLogLinesToSave +
            ", serviceFinishedTailLog='" + serviceFinishedTailLog + '\'' +
            ", s3UploaderKeyPattern='" + s3UploaderKeyPattern + '\'' +
            ", s3UploaderBucket='" + s3UploaderBucket + '\'' +
            ", useLocalDownloadService=" + useLocalDownloadService +
            ", localDownloadServiceTimeoutMillis=" + localDownloadServiceTimeoutMillis +
            ", maxTaskThreads=" + maxTaskThreads +
            ", dockerPrefix='" + dockerPrefix + '\'' +
            ", dockerStopTimeout=" + dockerStopTimeout +
            ", cgroupsMesosCpuTasksFormat='" + cgroupsMesosCpuTasksFormat + '\'' +
            ", procCgroupFormat='" + procCgroupFormat + '\'' +
            ", switchUserCommandFormat='" + switchUserCommandFormat + '\'' +
            ", artifactSignatureVerificationCommand=" + artifactSignatureVerificationCommand +
            ", failTaskOnInvalidArtifactSignature=" + failTaskOnInvalidArtifactSignature +
            ", signatureVerifyOut='" + signatureVerifyOut + '\'' +
            ']';
  }

  @Override
  public void updateFromProperties(Properties properties) {
    final Splitter commaSplitter = Splitter.on(',').omitEmptyStrings().trimResults();

    if (properties.containsKey(SHUTDOWN_TIMEOUT_MILLIS)) {
      setShutdownTimeoutWaitMillis(Long.parseLong(properties.getProperty(SHUTDOWN_TIMEOUT_MILLIS)));
    }

    if (properties.containsKey(HARD_KILL_AFTER_MILLIS)) {
      setHardKillAfterMillis(Long.parseLong(properties.getProperty(HARD_KILL_AFTER_MILLIS)));
    }

    if (properties.containsKey(NUM_CORE_KILL_THREADS)) {
      setKillThreads(Integer.parseInt(properties.getProperty(NUM_CORE_KILL_THREADS)));
    }

    if (properties.containsKey(NUM_CORE_THREAD_CHECK_THREADS)) {
      setThreadCheckThreads(Integer.parseInt(properties.getProperty(NUM_CORE_THREAD_CHECK_THREADS)));
    }

    if (properties.containsKey(CHECK_THREADS_EVERY_MILLIS)) {
      setCheckThreadsEveryMillis(Long.parseLong(properties.getProperty(CHECK_THREADS_EVERY_MILLIS)));
    }

    if (properties.containsKey(MAX_TASK_MESSAGE_LENGTH)) {
      setMaxTaskMessageLength(Integer.parseInt(properties.getProperty(MAX_TASK_MESSAGE_LENGTH)));
    }

    if (properties.containsKey(IDLE_EXECUTOR_SHUTDOWN_AFTER_MILLIS)) {
      setIdleExecutorShutdownWaitMillis(Long.parseLong(properties.getProperty(IDLE_EXECUTOR_SHUTDOWN_AFTER_MILLIS)));
    }

    if (properties.containsKey(SHUTDOWN_STOP_DRIVER_AFTER_MILLIS)) {
      setShutdownTimeoutWaitMillis(Long.parseLong(properties.getProperty(SHUTDOWN_STOP_DRIVER_AFTER_MILLIS)));
    }

    if (properties.containsKey(TASK_APP_DIRECTORY)) {
      setTaskAppDirectory(properties.getProperty(TASK_APP_DIRECTORY));
    }

    if (properties.containsKey(TASK_EXECUTOR_JAVA_LOG_PATH)) {
      setExecutorJavaLog(properties.getProperty(TASK_EXECUTOR_JAVA_LOG_PATH));
    }

    if (properties.containsKey(TASK_EXECUTOR_BASH_LOG_PATH)) {
      setExecutorBashLog(properties.getProperty(TASK_EXECUTOR_BASH_LOG_PATH));
    }

    if (properties.containsKey(TASK_SERVICE_LOG_PATH)) {
      setServiceLog(properties.getProperty(TASK_SERVICE_LOG_PATH));
    }

    if (properties.containsKey(DEFAULT_USER)) {
      setDefaultRunAsUser(properties.getProperty(DEFAULT_USER));
    }

    if (properties.containsKey(GLOBAL_TASK_DEFINITION_DIRECTORY)) {
      setGlobalTaskDefinitionDirectory(properties.getProperty(GLOBAL_TASK_DEFINITION_DIRECTORY));
    }

    if (properties.containsKey(GLOBAL_TASK_DEFINITION_SUFFIX)) {
      setGlobalTaskDefinitionSuffix(properties.getProperty(GLOBAL_TASK_DEFINITION_SUFFIX));
    }

    if (properties.containsKey(LOGROTATE_COMMAND)) {
      setLogrotateCommand(properties.getProperty(LOGROTATE_COMMAND));
    }

    if (properties.containsKey(LOGROTATE_CONFIG_DIRECTORY)) {
      setLogrotateConfDirectory(properties.getProperty(LOGROTATE_CONFIG_DIRECTORY));
    }

    if (properties.containsKey(LOGROTATE_STATE_FILE)) {
      setLogrotateStateFile(properties.getProperty(LOGROTATE_STATE_FILE));
    }

    if (properties.containsKey(LOGROTATE_DIRECTORY)) {
      setLogrotateToDirectory(properties.getProperty(LOGROTATE_DIRECTORY));
    }

    if (properties.containsKey(LOGROTATE_MAXAGE_DAYS)) {
      setLogrotateMaxageDays(Integer.parseInt(properties.getProperty(LOGROTATE_MAXAGE_DAYS)));
    }

    if (properties.containsKey(LOGROTATE_COUNT)) {
      setLogrotateCount(Integer.parseInt(properties.getProperty(LOGROTATE_COUNT)));
    }

    if (properties.containsKey(LOGROTATE_DATEFORMAT)) {
      setLogrotateDateformat(properties.getProperty(LOGROTATE_DATEFORMAT));
    }

    if (properties.containsKey(LOGROTATE_EXTRAS_DATEFORMAT)) {
      setLogrotateExtrasDateformat(properties.getProperty(LOGROTATE_EXTRAS_DATEFORMAT));
    }

    if (properties.containsKey(LOGROTATE_EXTRAS_FILES)) {
      setLogrotateAdditionalFiles(commaSplitter.splitToList(properties.getProperty(LOGROTATE_EXTRAS_FILES)));
    }

    if (properties.containsKey(TAIL_LOG_LINES_TO_SAVE)) {
      setTailLogLinesToSave(Integer.parseInt(properties.getProperty(TAIL_LOG_LINES_TO_SAVE)));
    }

    if (properties.containsKey(TAIL_LOG_FILENAME)) {
      setServiceFinishedTailLog(properties.getProperty(TAIL_LOG_FILENAME));
    }

    if (properties.containsKey(S3_FILES_TO_BACKUP)) {
      setS3UploaderAdditionalFiles(commaSplitter.splitToList(properties.getProperty(S3_FILES_TO_BACKUP)));
    }

    if (properties.containsKey(S3_UPLOADER_PATTERN)) {
      setS3UploaderKeyPattern(properties.getProperty(S3_UPLOADER_PATTERN));
    }

    if (properties.containsKey(S3_UPLOADER_BUCKET)) {
      setS3UploaderBucket(properties.getProperty(S3_UPLOADER_BUCKET));
    }

    if (properties.containsKey(USE_LOCAL_DOWNLOAD_SERVICE)) {
      setUseLocalDownloadService(Boolean.parseBoolean(properties.getProperty(USE_LOCAL_DOWNLOAD_SERVICE)));
    }

    if (properties.containsKey(LOCAL_DOWNLOAD_SERVICE_TIMEOUT_MILLIS)) {
      setLocalDownloadServiceTimeoutMillis(Long.parseLong(properties.getProperty(LOCAL_DOWNLOAD_SERVICE_TIMEOUT_MILLIS)));
    }

    if (properties.containsKey(MAX_TASK_THREADS)) {
      setMaxTaskThreads(Optional.of(Integer.parseInt(properties.getProperty(MAX_TASK_THREADS))));
    }

    if (properties.containsKey(DOCKER_PREFIX)) {
      setDockerPrefix(properties.getProperty(DOCKER_PREFIX));
    }

    if (properties.containsKey(DOCKER_STOP_TIMEOUT)) {
      setDockerStopTimeout(Integer.parseInt(properties.getProperty(DOCKER_STOP_TIMEOUT)));
    }
  }
}
