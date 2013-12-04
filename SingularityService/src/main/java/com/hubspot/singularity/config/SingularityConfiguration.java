package com.hubspot.singularity.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
  
  @Valid
  @NotNull
  private DataSourceFactory database;

  @NotNull
  private long cleanupEverySeconds = 5;
  
  @NotNull
  private long saveStateEverySeconds = 60;
  
  @NotNull
  private long killDecomissionedTasksAfterNewTasksSeconds = 300;
  
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

}
