package com.hubspot.singularity.config;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.google.common.collect.ImmutableList;

public class EmailConfiguration {

  @NotNull
  private List<EmailConfigurationEnum> taskLost = ImmutableList.of(EmailConfigurationEnum.ADMINS);
  @NotNull
  private List<EmailConfigurationEnum> taskFailed = ImmutableList.of(EmailConfigurationEnum.ADMINS, EmailConfigurationEnum.OWNERS);
  @NotNull
  private List<EmailConfigurationEnum> taskFinished = Collections.emptyList();
  @NotNull
  private List<EmailConfigurationEnum> taskKilled = ImmutableList.of(EmailConfigurationEnum.ADMINS);
  @NotNull
  private List<EmailConfigurationEnum> requestCooldown = ImmutableList.of(EmailConfigurationEnum.ADMINS, EmailConfigurationEnum.OWNERS);
  @NotNull
  private List<EmailConfigurationEnum> abort = ImmutableList.of(EmailConfigurationEnum.ADMINS);

  public List<EmailConfigurationEnum> getAbort() {
    return abort;
  }

  public void setAbort(List<EmailConfigurationEnum> abort) {
    this.abort = abort;
  }

  public List<EmailConfigurationEnum> getTaskLost() {
    return taskLost;
  }

  public void setTaskLost(List<EmailConfigurationEnum> taskLost) {
    this.taskLost = taskLost;
  }

  public List<EmailConfigurationEnum> getTaskFailed() {
    return taskFailed;
  }

  public void setTaskFailed(List<EmailConfigurationEnum> taskFailed) {
    this.taskFailed = taskFailed;
  }

  public List<EmailConfigurationEnum> getTaskFinished() {
    return taskFinished;
  }

  public void setTaskFinished(List<EmailConfigurationEnum> taskFinished) {
    this.taskFinished = taskFinished;
  }

  public List<EmailConfigurationEnum> getTaskKilled() {
    return taskKilled;
  }

  public void setTaskKilled(List<EmailConfigurationEnum> taskKilled) {
    this.taskKilled = taskKilled;
  }

  public List<EmailConfigurationEnum> getRequestCooldown() {
    return requestCooldown;
  }

  public void setRequestCooldown(List<EmailConfigurationEnum> requestCooldown) {
    this.requestCooldown = requestCooldown;
  }

}
