package com.hubspot.singularity.executor.config;

public class SingularityExecutorDockerAuthConfig {

  private boolean fromDockerConfig;

  private String username;
  private String password;
  private String email;
  private String serverAddress;

  public boolean isFromDockerConfig() {
    return fromDockerConfig;
  }

  public void setFromDockerConfig(boolean fromDockerConfig) {
    this.fromDockerConfig = fromDockerConfig;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getServerAddress() {
    return serverAddress;
  }

  public void setServerAddress(String serverAddress) {
    this.serverAddress = serverAddress;
  }

}
