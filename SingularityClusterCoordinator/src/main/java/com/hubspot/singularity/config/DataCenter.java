package com.hubspot.singularity.config;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityClientCredentials;

public class DataCenter {
  @NotNull
  private String name;
  @NotEmpty
  private List<String> hosts;
  @NotNull
  private String contextPath;
  // http or https
  private String scheme = "http";

  private Optional<SingularityClientCredentials> clientCredentials = Optional.absent();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getHosts() {
    return hosts;
  }

  public void setHosts(List<String> hosts) {
    this.hosts = hosts;
  }

  public String getContextPath() {
    return contextPath;
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public Optional<SingularityClientCredentials> getClientCredentials() {
    return clientCredentials;
  }

  public void setClientCredentials(Optional<SingularityClientCredentials> clientCredentials) {
    this.clientCredentials = clientCredentials;
  }

  @Override
  public String toString() {
    return "DataCenter{" +
        "name='" + name + '\'' +
        ", hosts=" + hosts +
        ", contextPath='" + contextPath + '\'' +
        ", scheme='" + scheme + '\'' +
        ", clientCredentials=" + clientCredentials +
        '}';
  }
}
