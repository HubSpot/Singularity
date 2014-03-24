package com.hubspot.singularity.config;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingularityConfiguration extends Configuration {

  @JsonProperty("mesos")
  private MesosConfiguration mesosConfiguration;
  
  @JsonProperty("zookeeper")
  private ZooKeeperConfiguration zooKeeperConfiguration;
  
  @JsonProperty("smtp")
  private SMTPConfiguration smtpConfiguration;

  @JsonProperty("hostname")
  private String hostname;
  
  @JsonProperty("singularityUIHostnameAndPath")
  private String singularityUIHostnameAndPath;
  
  @Valid
  @NotNull
  private DataSourceFactory database;

  @NotNull
  private long closeWaitSeconds = 5;
  
  @NotNull
  private long cleanupEverySeconds = 5;
  
  @NotNull
  private long checkDeploysEverySeconds = 5;
  
  @NotNull
  private long saveStateEverySeconds = 60;
  
  @NotNull
  private long defaultHealthcheckTimeoutSeconds = 5;
  
  @NotNull
  private long healthcheckIntervalSeconds = 5;
  
  @NotNull
  private long killScheduledTasksWithAreDecomissionedAfterSeconds = 300;
  
  @NotNull
  private long deltaAfterWhichTasksAreLateMillis = TimeUnit.SECONDS.toMillis(30);
  
  @NotNull
  private long warnAfterTasksDoNotRunDefaultSeconds = 300;
  
  @NotNull
  private long killAfterTasksDoNotRunDefaultSeconds = 600;
  
  @NotNull
  private int logFetchCoreThreads = 3;
  
  @NotNull
  private int healthcheckStartThreads = 3;
  
  @NotNull
  private int logFetchMaxThreads = 25;
  
  @NotNull
  private long zookeeperAsyncTimeout = 5000;
  
  @NotNull
  private long deployHealthyBySeconds = 30;
  
  public long getHealthcheckIntervalSeconds() {
    return healthcheckIntervalSeconds;
  }

  public void setHealthcheckIntervalSeconds(long healthcheckIntervalSeconds) {
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
  }
  
  public long getKillScheduledTasksWithAreDecomissionedAfterSeconds() {
    return killScheduledTasksWithAreDecomissionedAfterSeconds;
  }

  public void setKillScheduledTasksWithAreDecomissionedAfterSeconds(long killScheduledTasksWithAreDecomissionedAfterSeconds) {
    this.killScheduledTasksWithAreDecomissionedAfterSeconds = killScheduledTasksWithAreDecomissionedAfterSeconds;
  }

  public long getDeployHealthyBySeconds() {
    return deployHealthyBySeconds;
  }
  
  public int getHealthcheckStartThreads() {
    return healthcheckStartThreads;
  }

  public void setHealthcheckStartThreads(int healthcheckStartThreads) {
    this.healthcheckStartThreads = healthcheckStartThreads;
  }

  public void setDeployHealthyBySeconds(long deployHealthyBySeconds) {
    this.deployHealthyBySeconds = deployHealthyBySeconds;
  }

  public long getCheckDeploysEverySeconds() {
    return checkDeploysEverySeconds;
  }

  public long getDefaultHealthcheckTimeoutSeconds() {
    return defaultHealthcheckTimeoutSeconds;
  }

  public void setDefaultHealthcheckTimeoutSeconds(long defaultHealthcheckTimeoutSeconds) {
    this.defaultHealthcheckTimeoutSeconds = defaultHealthcheckTimeoutSeconds;
  }

  public long getCleanupEverySeconds() {
    return cleanupEverySeconds;
  }

  public long getCloseWaitSeconds() {
    return closeWaitSeconds;
  }

  @JsonProperty("database")
  public DataSourceFactory getDataSourceFactory() {
    return database;
  }

  public long getDeltaAfterWhichTasksAreLateMillis() {
    return deltaAfterWhichTasksAreLateMillis;
  }

  public String getHostname() {
    return hostname;
  }

  public long getKillAfterTasksDoNotRunDefaultSeconds() {
    return killAfterTasksDoNotRunDefaultSeconds;
  }
  
  public int getLogFetchCoreThreads() {
    return logFetchCoreThreads;
  }

  public int getLogFetchMaxThreads() {
    return logFetchMaxThreads;
  }

  public MesosConfiguration getMesosConfiguration() {
    return mesosConfiguration;
  }

  public long getSaveStateEverySeconds() {
    return saveStateEverySeconds;
  }

  public Optional<String> getSingularityUIHostnameAndPath() {
    return Optional.fromNullable(singularityUIHostnameAndPath);
  }

  public Optional<SMTPConfiguration> getSmtpConfiguration() {
    return Optional.fromNullable(smtpConfiguration);
  }

  public long getWarnAfterTasksDoNotRunDefaultSeconds() {
    return warnAfterTasksDoNotRunDefaultSeconds;
  }

  public long getZookeeperAsyncTimeout() {
    return zookeeperAsyncTimeout;
  }

  public ZooKeeperConfiguration getZooKeeperConfiguration() {
    return zooKeeperConfiguration;
  }

  public void setCheckDeploysEverySeconds(long checkDeploysEverySeconds) {
    this.checkDeploysEverySeconds = checkDeploysEverySeconds;
  }

  public void setCleanupEverySeconds(long cleanupEverySeconds) {
    this.cleanupEverySeconds = cleanupEverySeconds;
  }

  public void setCloseWaitSeconds(long closeWaitSeconds) {
    this.closeWaitSeconds = closeWaitSeconds;
  }

  @JsonProperty("database")
  public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
    this.database = dataSourceFactory;
  }

  public void setDeltaAfterWhichTasksAreLateMillis(long deltaAfterWhichTasksAreLateMillis) {
    this.deltaAfterWhichTasksAreLateMillis = deltaAfterWhichTasksAreLateMillis;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public void setKillAfterTasksDoNotRunDefaultSeconds(long killAfterTasksDoNotRunDefaultSeconds) {
    this.killAfterTasksDoNotRunDefaultSeconds = killAfterTasksDoNotRunDefaultSeconds;
  }

  public void setLogFetchCoreThreads(int logFetchCoreThreads) {
    this.logFetchCoreThreads = logFetchCoreThreads;
  }

  public void setLogFetchMaxThreads(int logFetchMaxThreads) {
    this.logFetchMaxThreads = logFetchMaxThreads;
  }

  public void setMesosConfiguration(MesosConfiguration mesosConfiguration) {
    this.mesosConfiguration = mesosConfiguration;
  }
  
  public void setSaveStateEverySeconds(long saveStateEverySeconds) {
    this.saveStateEverySeconds = saveStateEverySeconds;
  }

  public void setSingularityUIHostnameAndPath(String singularityUIHostnameAndPath) {
    this.singularityUIHostnameAndPath = singularityUIHostnameAndPath;
  }

  public void setSmtpConfiguration(SMTPConfiguration smtpConfiguration) {
    this.smtpConfiguration = smtpConfiguration;
  }

  public void setWarnAfterTasksDoNotRunDefaultSeconds(long warnAfterTasksDoNotRunDefaultSeconds) {
    this.warnAfterTasksDoNotRunDefaultSeconds = warnAfterTasksDoNotRunDefaultSeconds;
  }

  public void setZookeeperAsyncTimeout(long zookeeperAsyncTimeout) {
    this.zookeeperAsyncTimeout = zookeeperAsyncTimeout;
  }

  public void setZooKeeperConfiguration(ZooKeeperConfiguration zooKeeperConfiguration) {
    this.zooKeeperConfiguration = zooKeeperConfiguration;
  }

}
