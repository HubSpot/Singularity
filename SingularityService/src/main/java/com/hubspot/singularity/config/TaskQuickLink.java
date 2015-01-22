package com.hubspot.singularity.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TaskQuickLink defines a "quick-link" button to show on the
 */
public class TaskQuickLink {
  /**
   * Name of the button label in the SingularityUI interface
   */
  @JsonProperty
  private final String name;
  /**
   * Handlebars string to parse to generate a URL for the quick link
   * (e.g. "http://{{TASK_HOST}}/{{TASK_REQUEST_ID}}")
   */
  @JsonProperty
  private final String url;

  @JsonCreator
  public TaskQuickLink(@JsonProperty("name") String name, @JsonProperty("url") String url) {
    this.name = name;
    this.url = url;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  /**
   * Create a JSON representation of this object for dumping into window.config
   * portion of SingularityUI.
   *
   * @return JSON representation of TaskQuickLink
   */
  public String toString() {
    return "{'name': {0}, 'url': {1}}".format(
        name.replaceAll("\"", "\\\""),
        url.replaceAll("\"", "\\\""));
  }
}
