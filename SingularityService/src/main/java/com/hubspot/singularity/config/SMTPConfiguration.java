package com.hubspot.singularity.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hubspot.singularity.SingularityEmailDestination;
import com.hubspot.singularity.SingularityEmailType;

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
  private String mailerDatePattern = "MMM dd h:mm:ss a zzz";

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

  @JsonProperty
  private Optional<String> taskLogErrorRegex = Optional.absent();

  @JsonProperty
  private Optional<Boolean> taskLogErrorRegexCaseSensitive = Optional.absent();

  @NotNull
  @JsonProperty
  private Long maxTaskLogSearchOffset = 100000L;

  @JsonProperty
  private TimeZone mailerTimeZone = TimeZone.getTimeZone("UTC");

  @JsonProperty
  private Optional<String> subjectPrefix = Optional.absent();

  @JsonProperty
  private Optional<String> uiBaseUrl = Optional.absent();

  @JsonProperty("emails")
  private Map<SingularityEmailType, List<SingularityEmailDestination>> emailConfiguration = Maps.newHashMap(ImmutableMap.<SingularityEmailType, List<SingularityEmailDestination>>builder()
      .put(SingularityEmailType.REQUEST_IN_COOLDOWN, ImmutableList.of(SingularityEmailDestination.ADMINS, SingularityEmailDestination.OWNERS))
      .put(SingularityEmailType.SINGULARITY_ABORTING, ImmutableList.of(SingularityEmailDestination.ADMINS))
      .put(SingularityEmailType.TASK_FAILED, ImmutableList.of(SingularityEmailDestination.ADMINS, SingularityEmailDestination.OWNERS, SingularityEmailDestination.ACTION_TAKER))
      .put(SingularityEmailType.TASK_LOST, ImmutableList.of(SingularityEmailDestination.ADMINS, SingularityEmailDestination.ACTION_TAKER))
      .put(SingularityEmailType.TASK_FINISHED_LONG_RUNNING, ImmutableList.of(SingularityEmailDestination.OWNERS, SingularityEmailDestination.ADMINS))
      .put(SingularityEmailType.TASK_FINISHED_ON_DEMAND, ImmutableList.of(SingularityEmailDestination.OWNERS, SingularityEmailDestination.ACTION_TAKER))
      .put(SingularityEmailType.TASK_SCHEDULED_OVERDUE_TO_FINISH, ImmutableList.of(SingularityEmailDestination.OWNERS, SingularityEmailDestination.ADMINS))
      .put(SingularityEmailType.TASK_KILLED_UNHEALTHY, ImmutableList.of(SingularityEmailDestination.OWNERS, SingularityEmailDestination.ADMINS))
      .put(SingularityEmailType.REQUEST_PAUSED, ImmutableList.of(SingularityEmailDestination.OWNERS, SingularityEmailDestination.ADMINS))
      .put(SingularityEmailType.REQUEST_REMOVED, ImmutableList.of(SingularityEmailDestination.OWNERS, SingularityEmailDestination.ADMINS))
      .put(SingularityEmailType.REQUEST_UNPAUSED, ImmutableList.of(SingularityEmailDestination.OWNERS, SingularityEmailDestination.ADMINS))
      .put(SingularityEmailType.REQUEST_SCALED, ImmutableList.of(SingularityEmailDestination.OWNERS))
      .put(SingularityEmailType.DISASTER_DETECTED, ImmutableList.of(SingularityEmailDestination.ADMINS))
      .build());

  public Map<SingularityEmailType, List<SingularityEmailDestination>> getEmailConfiguration() {
    return emailConfiguration;
  }

  public void setEmailConfiguration(Map<SingularityEmailType, List<SingularityEmailDestination>> emailConfiguration) {
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

  public String getMailerDatePattern() {
    return mailerDatePattern;
  }

  public void setMailerDatePattern(String mailerDatePattern) {
    this.mailerDatePattern = mailerDatePattern;
  }

  public TimeZone getMailerTimeZone() {
    return mailerTimeZone;
  }

  public void setMailerTimeZone(TimeZone mailerTimeZone) {
    this.mailerTimeZone = mailerTimeZone;
  }

  public Optional<String> getTaskLogErrorRegex() { return taskLogErrorRegex; }

  public void setTaskLogErrorRegex(Optional<String> taskLogErrorRegex) { this.taskLogErrorRegex = taskLogErrorRegex; }

  public Optional<Boolean> getTaskLogErrorRegexCaseSensitive() { return taskLogErrorRegexCaseSensitive; }

  public void setTaskLogErrorRegexCaseSensitive(Optional<Boolean> taskLogErrorRegexCaseSensitive) { this.taskLogErrorRegexCaseSensitive = taskLogErrorRegexCaseSensitive; }

  public Long getMaxTaskLogSearchOffset() { return maxTaskLogSearchOffset; }

  public void setMaxTaskLogSearchOffset(Long maxTaskLogSearchOffset) { this.maxTaskLogSearchOffset = maxTaskLogSearchOffset; }

  public Optional<String> getSubjectPrefix() {
    return subjectPrefix;
  }

  public void setSubjectPrefix(Optional<String> subjectPrefix) {
    this.subjectPrefix = subjectPrefix;
  }

  public Optional<String> getUiBaseUrl() {
    return uiBaseUrl;
  }

  public void setUiBaseUrl(Optional<String> uiBaseUrl) {
    this.uiBaseUrl = uiBaseUrl;
  }
}
