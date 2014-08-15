package com.hubspot.singularity.config;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class UIConfiguration {
  public static final String DEFAULT_TITLE = "Singularity";

  @NotEmpty
  private String title = DEFAULT_TITLE;

  private String baseUrl;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Optional<String> getBaseUrl() {
    return Optional.fromNullable(Strings.emptyToNull(baseUrl));
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }
}
