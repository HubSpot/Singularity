package com.hubspot.singularity.config;

import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class UIConfiguration {

  @NotEmpty
  @JsonProperty
  private String title = "Singularity";

  @JsonProperty
  @Pattern( regexp = "^|#[0-9a-fA-F]{6}$" )
  private String navColor = "";

  @JsonProperty
  private String baseUrl;

  @JsonProperty
  private List<HashMap<String, String>> customQuickLinks = new LinkedList<>();

  private boolean hideNewDeployButton = false;
  private boolean hideNewRequestButton = false;

  public boolean isHideNewDeployButton() {
    return hideNewDeployButton;
  }

  public void setHideNewDeployButton(boolean hideNewDeployButton) {
    this.hideNewDeployButton = hideNewDeployButton;
  }

  public boolean isHideNewRequestButton() {
    return hideNewRequestButton;
  }

  public void setHideNewRequestButton(boolean hideNewRequestButton) {
    this.hideNewRequestButton = hideNewRequestButton;
  }

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

  public String getNavColor() {
    return navColor;
  }

  public void setNavColor(String navColor) {
    this.navColor = navColor;
  }

  public List<HashMap<String, String>> getCustomQuickLinks() {
    return customQuickLinks;
  }

  public void setCustomQuickLinks(List<HashMap<String, String>> customQuickLinks) {
    this.customQuickLinks = customQuickLinks;
  }
}
