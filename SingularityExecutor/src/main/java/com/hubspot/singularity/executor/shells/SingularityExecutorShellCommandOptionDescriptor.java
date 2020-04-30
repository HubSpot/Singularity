package com.hubspot.singularity.executor.shells;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;

public class SingularityExecutorShellCommandOptionDescriptor {
  @JsonProperty
  @NotNull
  private String name;

  @JsonProperty
  @NotNull
  private String flag;

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getFlag() {
    return flag;
  }

  public void setFlag(String flag) {
    this.flag = flag;
  }
}
