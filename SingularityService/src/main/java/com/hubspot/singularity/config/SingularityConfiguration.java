package com.hubspot.singularity.config;

import java.util.concurrent.TimeUnit;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

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
  private long saveStateEverySeconds = 60;
  
  @NotNull
  private long killDecomissionedTasksAfterNewTasksSeconds = 300;
  
  @NotNull
  private long deltaAfterWhichTasksAreLateMillis = TimeUnit.SECONDS.toMillis(30);
  
  @NotNull
  private long warnAfterTasksDoNotRunDefaultSeconds = 300;
  
  @NotNull
  private long killAfterTasksDoNotRunDefaultSeconds = 600;
  
  @NotNull
  private int logFetchCoreThreads = 3;
  
  @NotNull
  private int logFetchMaxThreads = 25;
    
  public int getLogFetchCoreThreads() {
    return logFetchCoreThreads;
  }

  public void setLogFetchCoreThreads(int logFetchCoreThreads) {
    this.logFetchCoreThreads = logFetchCoreThreads;
  }

  public int getLogFetchMaxThreads() {
    return logFetchMaxThreads;
  }

  public void setLogFetchMaxThreads(int logFetchMaxThreads) {
    this.logFetchMaxThreads = logFetchMaxThreads;
  }

  public long getWarnAfterTasksDoNotRunDefaultSeconds() {
    return warnAfterTasksDoNotRunDefaultSeconds;
  }

  public void setWarnAfterTasksDoNotRunDefaultSeconds(long warnAfterTasksDoNotRunDefaultSeconds) {
    this.warnAfterTasksDoNotRunDefaultSeconds = warnAfterTasksDoNotRunDefaultSeconds;
  }

  public long getKillAfterTasksDoNotRunDefaultSeconds() {
    return killAfterTasksDoNotRunDefaultSeconds;
  }

  public void setKillAfterTasksDoNotRunDefaultSeconds(long killAfterTasksDoNotRunDefaultSeconds) {
    this.killAfterTasksDoNotRunDefaultSeconds = killAfterTasksDoNotRunDefaultSeconds;
  }

  public long getCloseWaitSeconds() {
    return closeWaitSeconds;
  }

  public long getDeltaAfterWhichTasksAreLateMillis() {
    return deltaAfterWhichTasksAreLateMillis;
  }

  public void setDeltaAfterWhichTasksAreLateMillis(long deltaAfterWhichTasksAreLateMillis) {
    this.deltaAfterWhichTasksAreLateMillis = deltaAfterWhichTasksAreLateMillis;
  }

  public void setCloseWaitSeconds(long closeWaitSeconds) {
    this.closeWaitSeconds = closeWaitSeconds;
  }

  public long getKillDecomissionedTasksAfterNewTasksSeconds() {
    return killDecomissionedTasksAfterNewTasksSeconds;
  }

  public void setKillDecomissionedTasksAfterNewTasksSeconds(long killDecomissionedTasksAfterNewTasksSeconds) {
    this.killDecomissionedTasksAfterNewTasksSeconds = killDecomissionedTasksAfterNewTasksSeconds;
  }

  public long getSaveStateEverySeconds() {
    return saveStateEverySeconds;
  }

  public void setSaveStateEverySeconds(long saveStateEverySeconds) {
    this.saveStateEverySeconds = saveStateEverySeconds;
  }

  public long getCleanupEverySeconds() {
    return cleanupEverySeconds;
  }

  public void setCleanupEverySeconds(long cleanupEverySeconds) {
    this.cleanupEverySeconds = cleanupEverySeconds;
  }

  @JsonProperty("database")
  public DataSourceFactory getDataSourceFactory() {
    return database;
  }

  @JsonProperty("database")
  public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
    this.database = dataSourceFactory;
  }

  public MesosConfiguration getMesosConfiguration() {
    return mesosConfiguration;
  }

  public void setMesosConfiguration(MesosConfiguration mesosConfiguration) {
    this.mesosConfiguration = mesosConfiguration;
  }

  public ZooKeeperConfiguration getZooKeeperConfiguration() {
    return zooKeeperConfiguration;
  }

  public void setZooKeeperConfiguration(ZooKeeperConfiguration zooKeeperConfiguration) {
    this.zooKeeperConfiguration = zooKeeperConfiguration;
  }
  
  public Optional<SMTPConfiguration> getSmtpConfiguration() {
    return Optional.fromNullable(smtpConfiguration);
  }

  public void setSmtpConfiguration(SMTPConfiguration smtpConfiguration) {
    this.smtpConfiguration = smtpConfiguration;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public Optional<String> getSingularityUIHostnameAndPath() {
    return Optional.fromNullable(singularityUIHostnameAndPath);
  }

  public void setSingularityUIHostnameAndPath(String singularityUIHostnameAndPath) {
    this.singularityUIHostnameAndPath = singularityUIHostnameAndPath;
  }

}
