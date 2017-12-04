package com.hubspot.singularity.config.shell;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShellCommandDescriptor {

  @NotEmpty
  @JsonProperty
  private String name;

  @JsonProperty
  private String description;

  @JsonProperty
  @NotNull
  private List<ShellCommandOptionDescriptor> options = Collections.emptyList();

  public String getName() {
    return name;
  }

  public ShellCommandDescriptor setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public ShellCommandDescriptor setDescription(String description) {
    this.description = description;
    return this;
  }

  public List<ShellCommandOptionDescriptor> getOptions() {
    return options;
  }

  public ShellCommandDescriptor setOptions(List<ShellCommandOptionDescriptor> options) {
    this.options = options;
    return this;
  }

  @Override
  public String toString() {
    return "ShellCommandDescriptor [name=" + name + ", description=" + description + ", options=" + options + "]";
  }

}
