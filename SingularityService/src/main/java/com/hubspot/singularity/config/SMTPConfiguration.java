package com.hubspot.singularity.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hubspot.singularity.config.EmailConfigurationEnums.EmailDestination;
import com.hubspot.singularity.config.EmailConfigurationEnums.EmailType;

public class SMTPConfiguration {

  @JsonProperty
  private String username;

  @JsonProperty
  private String password;

  @JsonProperty
  private int taskLogLength = 512;

  @NotNull
  @JsonProperty
  private String host = "localhost";

  @JsonProperty
  private Integer port = 25; // SMTP

  @JsonProperty
  @NotNull
  private String from = "singularity-no-reply@example.com";

  @JsonProperty
  private int mailMaxThreads = 3;

  @NotNull
  @JsonProperty
  private boolean ssl = false;

  @NotNull
  @JsonProperty
  private boolean startTLS = false;

  @NotNull
  @JsonProperty
  private List<String> admins = Collections.emptyList();

  @JsonProperty
  private int rateLimitAfterNotifications = 5;

  @JsonProperty
  private long rateLimitPeriodMillis = TimeUnit.MINUTES.toMillis(10);

  @JsonProperty
  private long rateLimitCooldownMillis = TimeUnit.HOURS.toMillis(1);

  // Files to tail when sending a task email.
  @JsonProperty
  private List<String> taskEmailTailFiles = Arrays.asList("stdout", "stderr");

  @JsonProperty("emails")
  private Map<EmailType, List<EmailDestination>> emailConfiguration = Maps.newHashMap(ImmutableMap.<EmailType, List<EmailDestination>>builder()
      .put(EmailType.REQUEST_IN_COOLDOWN, ImmutableList.of(EmailDestination.ADMINS, EmailDestination.OWNERS))
      .put(EmailType.SINGULARITY_ABORTING, ImmutableList.of(EmailDestination.ADMINS))
      .put(EmailType.TASK_FAILED, ImmutableList.of(EmailDestination.ADMINS, EmailDestination.OWNERS, EmailDestination.ACTION_TAKER))
      .put(EmailType.TASK_LOST, ImmutableList.of(EmailDestination.ADMINS, EmailDestination.ACTION_TAKER))
      .put(EmailType.TASK_FINISHED_LONG_RUNNING, ImmutableList.of(EmailDestination.OWNERS, EmailDestination.ADMINS))
      .put(EmailType.TASK_FINISHED_ON_DEMAND, ImmutableList.of(EmailDestination.OWNERS, EmailDestination.ACTION_TAKER))
      .put(EmailType.TASK_SCHEDULED_OVERDUE_TO_FINISH, ImmutableList.of(EmailDestination.OWNERS, EmailDestination.ADMINS))
      .put(EmailType.TASK_KILLED_UNHEALTHY, ImmutableList.of(EmailDestination.OWNERS, EmailDestination.ADMINS))
      .put(EmailType.REQUEST_PAUSED, ImmutableList.of(EmailDestination.OWNERS, EmailDestination.ADMINS))
      .put(EmailType.REQUEST_REMOVED, ImmutableList.of(EmailDestination.OWNERS, EmailDestination.ADMINS))
      .put(EmailType.REQUEST_UNPAUSED, ImmutableList.of(EmailDestination.OWNERS, EmailDestination.ADMINS))
      .put(EmailType.REQUEST_SCALED, ImmutableList.of(EmailDestination.OWNERS))
      .build());

  public Map<EmailType, List<EmailDestination>> getEmailConfiguration() {
    return emailConfiguration;
  }

  public void setEmailConfiguration(Map<EmailType, List<EmailDestination>> emailConfiguration) {
    this.emailConfiguration.putAll(emailConfiguration);
  }

  public int getTaskLogLength() {
    return taskLogLength;
  }

  public void setTaskLogLength(int length) {
    taskLogLength = length;
  }

  public Optional<String> getUsername() {
    return Optional.fromNullable(username);
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public int getMailMaxThreads() {
    return mailMaxThreads;
  }

  public void setMailMaxThreads(int mailMaxThreads) {
    this.mailMaxThreads = mailMaxThreads;
  }

  public Optional<String> getPassword() {
    return Optional.fromNullable(password);
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public boolean isSsl() {
    return ssl;
  }

  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }

  public boolean isStartTLS() {
    return startTLS;
  }

  public void setStartTLS(boolean startTLS) {
    this.startTLS = startTLS;
  }

  public List<String> getAdmins() {
    return admins;
  }

  public void setAdmins(List<String> admins) {
    this.admins = admins;
  }

  public int getRateLimitAfterNotifications() {
    return rateLimitAfterNotifications;
  }

  public void setRateLimitAfterNotifications(int rateLimitAfterNotifications) {
    this.rateLimitAfterNotifications = rateLimitAfterNotifications;
  }

  public long getRateLimitPeriodMillis() {
    return rateLimitPeriodMillis;
  }

  public void setRateLimitPeriodMillis(long rateLimitPeriodMillis) {
    this.rateLimitPeriodMillis = rateLimitPeriodMillis;
  }

  public long getRateLimitCooldownMillis() {
    return rateLimitCooldownMillis;
  }

  public void setRateLimitCooldownMillis(long rateLimitCooldownMillis) {
    this.rateLimitCooldownMillis = rateLimitCooldownMillis;
  }

  public List<String> getTaskEmailTailFiles() {
    return taskEmailTailFiles;
  }

  public void setTaskEmailTailFiles(List<String> taskEmailTailFiles) {
    this.taskEmailTailFiles = taskEmailTailFiles;
  }
}
