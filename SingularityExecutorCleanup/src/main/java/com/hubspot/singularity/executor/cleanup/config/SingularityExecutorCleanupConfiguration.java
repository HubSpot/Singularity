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

  @NotEmpty
  @JsonProperty
  private String singularityContextPath = "";

  @JsonProperty
  private boolean runDockerCleanup = false;

  @NotNull
  @JsonProperty
  private Optional<SingularityClientCredentials> singularityClientCredentials = Optional.absent();

  @JsonProperty
  private boolean cleanTasksWhenDecommissioned = true;

  @NotNull
  @JsonProperty
  private CompressionType compressionType = CompressionType.GZIP;

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

  @Override
  public String toString() {
    return "SingularityExecutorCleanupConfiguration[" +
            "safeModeWontRunWithNoTasks=" + safeModeWontRunWithNoTasks +
            ", executorCleanupResultsDirectory='" + executorCleanupResultsDirectory + '\'' +
            ", executorCleanupResultsSuffix='" + executorCleanupResultsSuffix + '\'' +
            ", cleanupAppDirectoryOfFailedTasksAfterMillis=" + cleanupAppDirectoryOfFailedTasksAfterMillis +
            ", singularityHosts=" + singularityHosts +
            ", singularityContextPath='" + singularityContextPath + '\'' +
            ", runDockerCleanup=" + runDockerCleanup +
            ", singularityClientCredentials=" + singularityClientCredentials +
            ", compressionType=" + compressionType +
            ']';
  }
}
