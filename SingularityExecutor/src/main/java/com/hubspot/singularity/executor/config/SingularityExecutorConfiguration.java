package com.hubspot.singularity.executor.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;
import com.hubspot.singularity.executor.models.ThreadCheckerType;
import com.hubspot.singularity.executor.shells.SingularityExecutorShellCommandDescriptor;
import com.hubspot.singularity.executor.utils.MesosUtils;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;
import com.hubspot.singularity.runner.base.constraints.DirectoryExists;

@Configuration(filename = "/etc/singularity.executor.yaml", consolidatedField = "executor")
public class SingularityExecutorConfiguration extends BaseRunnerConfiguration {
  @NotEmpty
  @JsonProperty
  private String executorJavaLog = "executor.java.log";

  @NotEmpty
  @JsonProperty
  private String executorBashLog = "executor.bash.log";

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
  private long initialIdleExecutorShutdownWaitMillis = TimeUnit.MINUTES.toMillis(1);

  @Min(0)
  @JsonProperty
  private long idleExecutorShutdownWaitMillis = TimeUnit.SECONDS.toMillis(10);

  @Min(0)
  @JsonProperty
  private long stopDriverAfterMillis = TimeUnit.SECONDS.toMillis(1);

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
  private String logrotateDateformat= "%Y%m%d%s";

  @NotEmpty
  @JsonProperty
  private String logrotateExtrasDateformat = "%Y%m%d";

  @NotNull
  @JsonProperty
  private LogrotateCompressionSettings logrotateCompressionSettings = LogrotateCompressionSettings.empty();

  @NotNull
  @JsonProperty
  private List<SingularityExecutorLogrotateAdditionalFile> logrotateAdditionalFiles = Collections.emptyList();

  @Min(1)
  @JsonProperty
  private int tailLogLinesToSave = 2500;

  @JsonProperty
  private boolean useLocalDownloadService = false;

  @Min(1)
  @JsonProperty
  private long localDownloadServiceTimeoutMillis = TimeUnit.MINUTES.toMillis(3);

  @Min(1)
  @JsonProperty
  private int localDownloadServiceMaxConnections = 25;

  @JsonProperty
  private Optional<Integer> maxTaskThreads = Optional.absent();

  @JsonProperty
  private String dockerPrefix = "se-";

  @Min(5)
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

  @JsonProperty
  public List<SingularityExecutorShellCommandDescriptor> shellCommands = Collections.emptyList();

  @NotEmpty
  @JsonProperty
  public String shellCommandOutFile = "executor.commands.{NAME}.{TIMESTAMP}.log";

  @NotEmpty
  @JsonProperty
  private String shellCommandPidPlaceholder = "{PID}";

  @NotEmpty
  @JsonProperty
  private String shellCommandUserPlaceholder = "{USER}";

  @NotEmpty
  @JsonProperty
  private String shellCommandPidFile = ".task-pid";

  @JsonProperty
  private List<String> shellCommandPrefix = Collections.emptyList();

  @JsonProperty
  private int dockerClientTimeLimitSeconds = 300;

  @JsonProperty
  private int dockerClientConnectionPoolSize = 5;

  @JsonProperty
  private int maxDockerPullAttempts = 2;

  @JsonProperty
  private Optional<SingularityExecutorDockerAuthConfig> dockerAuthConfig = Optional.absent();

  @JsonProperty
  private ThreadCheckerType threadCheckerType = ThreadCheckerType.PS;

  @JsonProperty
  private SingularityExecutorLogrotateFrequency logrotateFrequency = SingularityExecutorLogrotateFrequency.DAILY;

  @NotEmpty
  @JsonProperty
  private String cronDirectory = "/etc/cron.d";

  @JsonProperty
  private boolean useFileAttributes = false;

  public SingularityExecutorConfiguration() {
    super(Optional.of("singularity-executor.log"));
  }

  public String getShellCommandPidPlaceholder() {
    return shellCommandPidPlaceholder;
  }

  public void setShellCommandPidPlaceholder(String shellCommandPidPlaceholder) {
    this.shellCommandPidPlaceholder = shellCommandPidPlaceholder;
  }

  public String getShellCommandUserPlaceholder() {
    return shellCommandUserPlaceholder;
  }

  public void setShellCommandUserPlaceholder(String shellCommandUserPlaceholder) {
    this.shellCommandUserPlaceholder = shellCommandUserPlaceholder;
  }

  public List<SingularityExecutorLogrotateAdditionalFile> getLogrotateAdditionalFiles() {
    return logrotateAdditionalFiles;
  }

  public void setLogrotateAdditionalFiles(List<SingularityExecutorLogrotateAdditionalFile> logrotateAdditionalFiles) {
    this.logrotateAdditionalFiles = logrotateAdditionalFiles;
  }

  public String getExecutorJavaLog() {
    return executorJavaLog;
  }

  public String getExecutorBashLog() {
    return executorBashLog;
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

  public int getLocalDownloadServiceMaxConnections() {
    return localDownloadServiceMaxConnections;
  }

  public void setLocalDownloadServiceMaxConnections(int localDownloadServiceMaxConnections) {
    this.localDownloadServiceMaxConnections = localDownloadServiceMaxConnections;
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
  public List<SingularityExecutorShellCommandDescriptor> getShellCommands() {
    return shellCommands;
  }

  public void setShellCommands(List<SingularityExecutorShellCommandDescriptor> shellCommands) {
    this.shellCommands = shellCommands;
  }

  public String getShellCommandOutFile() {
    return shellCommandOutFile;
  }

  public void setShellCommandOutFile(String shellCommandOutFile) {
    this.shellCommandOutFile = shellCommandOutFile;
  }

  public String getShellCommandPidFile() {
    return shellCommandPidFile;
  }

  public void setShellCommandPidFile(String shellCommandPidFile) {
    this.shellCommandPidFile = shellCommandPidFile;
  }

  public List<String> getShellCommandPrefix() {
    return shellCommandPrefix;
  }

  public void setShellCommandPrefix(List<String> shellCommandPrefix) {
    this.shellCommandPrefix = shellCommandPrefix;
  }

  public int getDockerClientTimeLimitSeconds() {
    return dockerClientTimeLimitSeconds;
  }

  public void setDockerClientTimeLimitSeconds(int dockerClientTimeLimitMs) {
    this.dockerClientTimeLimitSeconds = dockerClientTimeLimitMs;
  }

  public int getDockerClientConnectionPoolSize() {
    return dockerClientConnectionPoolSize;
  }

  public void setDockerClientConnectionPoolSize(int dockerClientConnectionPoolSize) {
    this.dockerClientConnectionPoolSize = dockerClientConnectionPoolSize;
  }

  public int getMaxDockerPullAttempts() {
    return maxDockerPullAttempts;
  }

  public void setMaxDockerPullAttempts(int maxDockerPullAttempts) {
    this.maxDockerPullAttempts = maxDockerPullAttempts;
  }

  public Optional<SingularityExecutorDockerAuthConfig> getDockerAuthConfig() {
    return dockerAuthConfig;
  }

  public void setDockerAuthConfig(Optional<SingularityExecutorDockerAuthConfig> dockerAuthConfig) {
    this.dockerAuthConfig = dockerAuthConfig;
  }

  public ThreadCheckerType getThreadCheckerType() {
    return threadCheckerType;
  }

  public void setThreadCheckerType(ThreadCheckerType threadCheckerType) {
    this.threadCheckerType = threadCheckerType;
  }

  public SingularityExecutorLogrotateFrequency getLogrotateFrequency() {
    return logrotateFrequency;
  }

  public void setLogrotateFrequency(SingularityExecutorLogrotateFrequency logrotateFrequency) {
    this.logrotateFrequency = logrotateFrequency;
  }

  public String getCronDirectory() {
    return cronDirectory;
  }

  public void setCronDirectory(String cronDirectory) {
    this.cronDirectory = cronDirectory;
  }

  public boolean isUseFileAttributes() {
    return useFileAttributes;
  }

  public SingularityExecutorConfiguration setUseFileAttributes(boolean useFileAttributes) {
    this.useFileAttributes = useFileAttributes;
    return this;
  }

  public LogrotateCompressionSettings getLogrotateCompressionSettings() {
    return logrotateCompressionSettings;
  }

  public void setLogrotateCompressionSettings(LogrotateCompressionSettings logrotateCompressionSettings) {
    this.logrotateCompressionSettings = logrotateCompressionSettings;
  }

  public long getInitialIdleExecutorShutdownWaitMillis() {
    return initialIdleExecutorShutdownWaitMillis;
  }

  public void setInitialIdleExecutorShutdownWaitMillis(long initialIdleExecutorShutdownWaitMillis) {
    this.initialIdleExecutorShutdownWaitMillis = initialIdleExecutorShutdownWaitMillis;
  }

  @Override
  public String toString() {
    return "SingularityExecutorConfiguration{" +
        "executorJavaLog='" + executorJavaLog + '\'' +
        ", executorBashLog='" + executorBashLog + '\'' +
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
        ", logrotateCompressionSettings=" + logrotateCompressionSettings +
        ", logrotateAdditionalFiles=" + logrotateAdditionalFiles +
        ", tailLogLinesToSave=" + tailLogLinesToSave +
        ", useLocalDownloadService=" + useLocalDownloadService +
        ", localDownloadServiceTimeoutMillis=" + localDownloadServiceTimeoutMillis +
        ", localDownloadServiceMaxConnections=" + localDownloadServiceMaxConnections +
        ", maxTaskThreads=" + maxTaskThreads +
        ", dockerPrefix='" + dockerPrefix + '\'' +
        ", dockerStopTimeout=" + dockerStopTimeout +
        ", cgroupsMesosCpuTasksFormat='" + cgroupsMesosCpuTasksFormat + '\'' +
        ", procCgroupFormat='" + procCgroupFormat + '\'' +
        ", switchUserCommandFormat='" + switchUserCommandFormat + '\'' +
        ", artifactSignatureVerificationCommand=" + artifactSignatureVerificationCommand +
        ", failTaskOnInvalidArtifactSignature=" + failTaskOnInvalidArtifactSignature +
        ", signatureVerifyOut='" + signatureVerifyOut + '\'' +
        ", shellCommands=" + shellCommands +
        ", shellCommandOutFile='" + shellCommandOutFile + '\'' +
        ", shellCommandPidPlaceholder='" + shellCommandPidPlaceholder + '\'' +
        ", shellCommandUserPlaceholder='" + shellCommandUserPlaceholder + '\'' +
        ", shellCommandPidFile='" + shellCommandPidFile + '\'' +
        ", shellCommandPrefix=" + shellCommandPrefix +
        ", dockerClientTimeLimitSeconds=" + dockerClientTimeLimitSeconds +
        ", dockerClientConnectionPoolSize=" + dockerClientConnectionPoolSize +
        ", maxDockerPullAttempts=" + maxDockerPullAttempts +
        ", dockerAuthConfig=" + dockerAuthConfig +
        ", threadCheckerType=" + threadCheckerType +
        ", logrotateFrequency=" + logrotateFrequency +
        ", cronDirectory='" + cronDirectory + '\'' +
        ", useFileAttributes=" + useFileAttributes +
        "} " + super.toString();
  }
}
