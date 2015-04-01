package com.hubspot.singularity.config.shell;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShellCommandOptionDescriptor {

  @NotEmpty
  @JsonProperty
  private String name;

  @JsonProperty
  private String description;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return "ShellCommandOptionDescriptor [name=" + name + ", description=" + description + "]";
  }

}
