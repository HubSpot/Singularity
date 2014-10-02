package com.hubspot.singularity.config;

import java.util.Optional;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.base.Strings;

public class UIConfiguration {

  @NotEmpty
  private String title = "Singularity";

  private String baseUrl;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Optional<String> getBaseUrl() {
    return Optional.ofNullable(Strings.emptyToNull(baseUrl));
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

}
