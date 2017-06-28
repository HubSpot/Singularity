package com.hubspot.singularity.executor.cleanup.config;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityClientCredentials;
import com.hubspot.singularity.SingularityS3UploaderFile;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;
import com.hubspot.singularity.runner.base.constraints.DirectoryExists;
import com.hubspot.singularity.runner.base.shared.CompressionType;

@Configuration(filename = "/etc/singularity.executor.cleanup.yaml", consolidatedField = "executorCleanup")
public class SingularityExecutorCleanupConfiguration extends BaseRunnerConfiguration {
  @JsonProperty
  private boolean safeModeWontRunWithNoTasks = true;

  @DirectoryExists
  @JsonProperty
  private String executorCleanupResultsDirectory;

  @NotEmpty
  @JsonProperty
  private String executorCleanupResultsSuffix = ".cleanup.json";

  @Min(1)
  @JsonProperty
  private long cleanupAppDirectoryOfFailedTasksAfterMillis = TimeUnit.DAYS.toMillis(1);

  @NotEmpty
  @JsonProperty
  private List<String> singularityHosts = Collections.emptyList();

  @JsonProperty
  private boolean singularityUseSsl = false;

  @NotEmpty
  @JsonProperty
  private String singularityContextPath = "";

  @JsonProperty
  private boolean runDockerCleanup = false;

  @JsonProperty
  private Optional<SingularityClientCredentials> singularityClientCredentials = Optional.absent();

  @JsonProperty
  private boolean cleanTasksWhenDecommissioned = true;

  @NotNull
  @JsonProperty
  private CompressionType compressionType = CompressionType.GZIP;

  @NotEmpty
  private String defaultServiceLog = "service.log";

  @NotEmpty
  private String defaultServiceFinishedTailLog = "tail_of_finished_service.log";

  @NotNull
  private List<SingularityS3UploaderFile> s3UploaderAdditionalFiles = Collections.singletonList(SingularityS3UploaderFile.fromString("service.log"));

  @NotNull
  private String defaultS3Bucket = "";

  /**
   * S3 Key format for finding logs. Should be the same as
   * configuration set for SingularityService
   */
  @NotNull
  private String s3KeyFormat = "%requestId/%Y/%m/%taskId_%index-%s-%filename";

  public SingularityExecutorCleanupConfiguration() {
    super(Optional.of("singularity-executor-cleanup.log"));
  }

  public boolean isSafeModeWontRunWithNoTasks() {
    return safeModeWontRunWithNoTasks;
  }

  public void setSafeModeWontRunWithNoTasks(boolean safeModeWontRunWithNoTasks) {
    this.safeModeWontRunWithNoTasks = safeModeWontRunWithNoTasks;
  }

  public String getExecutorCleanupResultsDirectory() {
    return executorCleanupResultsDirectory;
  }

  public void setExecutorCleanupResultsDirectory(String executorCleanupResultsDirectory) {
    this.executorCleanupResultsDirectory = executorCleanupResultsDirectory;
  }

  public String getExecutorCleanupResultsSuffix() {
    return executorCleanupResultsSuffix;
  }

  public void setExecutorCleanupResultsSuffix(String executorCleanupResultsSuffix) {
    this.executorCleanupResultsSuffix = executorCleanupResultsSuffix;
  }

  public long getCleanupAppDirectoryOfFailedTasksAfterMillis() {
    return cleanupAppDirectoryOfFailedTasksAfterMillis;
  }

  public void setCleanupAppDirectoryOfFailedTasksAfterMillis(long cleanupAppDirectoryOfFailedTasksAfterMillis) {
    this.cleanupAppDirectoryOfFailedTasksAfterMillis = cleanupAppDirectoryOfFailedTasksAfterMillis;
  }

  public List<String> getSingularityHosts() {
    return singularityHosts;
  }

  public void setSingularityHosts(List<String> singularityHosts) {
    this.singularityHosts = singularityHosts;
  }

  public boolean isSingularityUseSsl() {
    return singularityUseSsl;
  }

  public void setSingularityUseSsl(boolean singularityUseSsl) {
    this.singularityUseSsl = singularityUseSsl;
  }

  public String getSingularityContextPath() {
    return singularityContextPath;
  }

  public void setSingularityContextPath(String singularityContextPath) {
    this.singularityContextPath = singularityContextPath;
  }

  public boolean isRunDockerCleanup() {
    return runDockerCleanup;
  }

  public void setRunDockerCleanup(boolean runDockerCleanup) {
    this.runDockerCleanup = runDockerCleanup;
  }

  public Optional<SingularityClientCredentials> getSingularityClientCredentials() {
    return singularityClientCredentials;
  }

  public void setSingularityClientCredentials(Optional<SingularityClientCredentials> singularityClientCredentials) {
    this.singularityClientCredentials = singularityClientCredentials;
  }

  public boolean isCleanTasksWhenDecommissioned() {
    return cleanTasksWhenDecommissioned;
  }

  public void setCleanTasksWhenDecommissioned(boolean cleanTasksWhenDecommissioned) {
    this.cleanTasksWhenDecommissioned = cleanTasksWhenDecommissioned;
  }

  public CompressionType getCompressionType() {
    return compressionType;
  }

  public void setCompressionType(CompressionType compressionType) {
    this.compressionType = compressionType;
  }

  public String getDefaultServiceLog() {
    return defaultServiceLog;
  }

  public SingularityExecutorCleanupConfiguration setDefaultServiceLog(String defaultServiceLog) {
    this.defaultServiceLog = defaultServiceLog;
    return this;
  }

  public String getDefaultServiceFinishedTailLog() {
    return defaultServiceFinishedTailLog;
  }

  public SingularityExecutorCleanupConfiguration setDefaultServiceFinishedTailLog(String defaultServiceFinishedTailLog) {
    this.defaultServiceFinishedTailLog = defaultServiceFinishedTailLog;
    return this;
  }

  public List<SingularityS3UploaderFile> getS3UploaderAdditionalFiles() {
    return s3UploaderAdditionalFiles;
  }

  public SingularityExecutorCleanupConfiguration setS3UploaderAdditionalFiles(List<SingularityS3UploaderFile> s3UploaderAdditionalFiles) {
    this.s3UploaderAdditionalFiles = s3UploaderAdditionalFiles;
    return this;
  }

  public String getDefaultS3Bucket() {
    return defaultS3Bucket;
  }

  public SingularityExecutorCleanupConfiguration setDefaultS3Bucket(String defaultS3Bucket) {
    this.defaultS3Bucket = defaultS3Bucket;
    return this;
  }

  public String getS3KeyFormat() {
    return s3KeyFormat;
  }

  public SingularityExecutorCleanupConfiguration setS3KeyFormat(String s3KeyFormat) {
    this.s3KeyFormat = s3KeyFormat;
    return this;
  }

  @Override
  public String toString() {
    return "SingularityExecutorCleanupConfiguration{" +
        "safeModeWontRunWithNoTasks=" + safeModeWontRunWithNoTasks +
        ", executorCleanupResultsDirectory='" + executorCleanupResultsDirectory + '\'' +
        ", executorCleanupResultsSuffix='" + executorCleanupResultsSuffix + '\'' +
        ", cleanupAppDirectoryOfFailedTasksAfterMillis=" + cleanupAppDirectoryOfFailedTasksAfterMillis +
        ", singularityHosts=" + singularityHosts +
        ", singularityUseSsl=" + singularityUseSsl +
        ", singularityContextPath='" + singularityContextPath + '\'' +
        ", runDockerCleanup=" + runDockerCleanup +
        ", singularityClientCredentials=" + singularityClientCredentials +
        ", cleanTasksWhenDecommissioned=" + cleanTasksWhenDecommissioned +
        ", compressionType=" + compressionType +
        ", defaultServiceLog='" + defaultServiceLog + '\'' +
        ", defaultServiceFinishedTailLog='" + defaultServiceFinishedTailLog + '\'' +
        ", s3UploaderAdditionalFiles=" + s3UploaderAdditionalFiles +
        ", defaultS3Bucket='" + defaultS3Bucket + '\'' +
        ", s3KeyFormat='" + s3KeyFormat + '\'' +
        "} " + super.toString();
  }
}
