package com.hubspot.singularity.config.shell;

import java.util.List;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShellCommandDescriptor {

  @NotEmpty
  @JsonProperty
  private String name;

  @JsonProperty
  private String description;

  @JsonProperty
  private List<ShellCommandOptionDescriptor> options;

}
