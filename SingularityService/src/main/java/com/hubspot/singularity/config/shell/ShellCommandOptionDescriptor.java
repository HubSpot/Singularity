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

  public ShellCommandOptionDescriptor setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public ShellCommandOptionDescriptor setDescription(String description) {
    this.description = description;
    return this;
  }

  @Override
  public String toString() {
    return "ShellCommandOptionDescriptor [name=" + name + ", description=" + description + "]";
  }

}
