package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityShellCommand {

  private final String name;
  private final Optional<List<String>> options;
  private final Optional<String> user;
  private final Optional<String> logfileName;

  public SingularityShellCommand(@JsonProperty("name") String name, @JsonProperty("options") Optional<List<String>> options, @JsonProperty("user") Optional<String> user, @JsonProperty("logfileName") Optional<String> logfileName) {
    this.name = name;
    this.options = options;
    this.user = user;
    this.logfileName = logfileName;
  }

  @ApiModelProperty(required=false, value="User who requested the shell command")
  public Optional<String> getUser() {
    return user;
  }

  @ApiModelProperty(required=true, value="Name of the shell command to run")
  public String getName() {
    return name;
  }

  @ApiModelProperty(required=false, value="Additional options related to the shell command")
  public Optional<List<String>> getOptions() {
    return options;
  }

  @ApiModelProperty(required=false, value="File name for shell command output")
  public Optional<String> getLogfileName() {
    return logfileName;
  }

  @Override public String toString() {
    return "SingularityShellCommand[" +
        "name='" + name + '\'' +
        ", options=" + options +
        ", user=" + user +
        ", logfileName=" + logfileName +
        ']';
  }
}
