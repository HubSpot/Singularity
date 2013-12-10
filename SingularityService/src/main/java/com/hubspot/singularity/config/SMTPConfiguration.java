package com.hubspot.singularity.config;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SMTPConfiguration {

  @JsonProperty
  private String username;

  @JsonProperty
  private String password;

  @NotNull
  @JsonProperty
  private String host = "localhost";

  @JsonProperty
  private Integer port;

  @JsonProperty
  @NotNull
  private String from = "singularity-no-reply@example.com";

  @NotNull
  @JsonProperty
  private int mailThreads = 1;
  
  @NotNull
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

  @JsonProperty("logging")
  private SMTPLoggingConfiguration smtpLoggingConfiguration = new SMTPLoggingConfiguration();
  
  public SMTPLoggingConfiguration getSmtpLoggingConfiguration() {
    return smtpLoggingConfiguration;
  }

  public void setSmtpLoggingConfiguration(SMTPLoggingConfiguration smtpLoggingConfiguration) {
    this.smtpLoggingConfiguration = smtpLoggingConfiguration;
  }

  public Optional<String> getUsername() {
    return Optional.fromNullable(username);
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public int getMailThreads() {
    return mailThreads;
  }

  public void setMailThreads(int mailThreads) {
    this.mailThreads = mailThreads;
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

  public Optional<Integer> getPort() {
    return Optional.fromNullable(port);
  }

  public void setPort(Integer port) {
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

}
  public Optional<String> getUsername() {
    return Optional.fromNullable(username);
  }

  public void setUsername(String username) {
    this.username = username;
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

  public Optional<Integer> getPort() {
    return Optional.fromNullable(port);
  }

  public void setPort(Integer port) {
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

}
