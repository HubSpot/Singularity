package com.hubspot.singularity.config.shell;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShellCommandOptionDescriptor {

  @NotEmpty
  @JsonProperty
  private String name;

  @JsonProperty
  private String description;

}
