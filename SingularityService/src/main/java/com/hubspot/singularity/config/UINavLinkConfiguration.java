package com.hubspot.singularity.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.annotation.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UINavLinkConfiguration {

  private String title;
  private String linkFormat;
  private Boolean divider = false;
  private String tooltip;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getLinkFormat() {
    return linkFormat;
  }

  public void setLinkFormat(String linkFormat) {
    this.linkFormat = linkFormat;
  }

  public Boolean getDivider() {
    return divider;
  }

  public void setDivider(Boolean divider) {
    this.divider = divider;
  }

  @Nullable
  public String getTooltip() {
    return tooltip;
  }

  public void setTooltip(@Nullable String tooltip) {
    this.tooltip = tooltip;
  }
}
