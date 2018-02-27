package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents the path to a specific task's Mesos sandbox")
public class SingularitySandbox {

  private final List<SingularitySandboxFile> files;
  private final String fullPathToRoot;
  private final String currentDirectory;
  private final String slaveHostname;

  @JsonCreator
  public SingularitySandbox(@JsonProperty("files") List<SingularitySandboxFile> files,
                            @JsonProperty("fullPathToRoot") String fullPathToRoot,
                            @JsonProperty("currentDirectory") String currentDirectory,
                            @JsonProperty("slaveHostname") String slaveHostname) {
    this.files = files;
    this.currentDirectory = currentDirectory;
    this.fullPathToRoot = fullPathToRoot;
    this.slaveHostname = slaveHostname;
  }

  @Schema(description = "Full path to the root of the Mesos task sandbox")
  public String getFullPathToRoot() {
    return fullPathToRoot;
  }

  @Schema(description = "Hostname of tasks's slave")
  public String getSlaveHostname() {
    return slaveHostname;
  }

  @Schema(description = "List of files inside sandbox")
  public List<SingularitySandboxFile> getFiles() {
    return files;
  }

  @Schema(description = "Current directory")
  public String getCurrentDirectory() {
    return currentDirectory;
  }

  @Override
  public String toString() {
    return "SingularitySandbox{" +
        "files=" + files +
        ", fullPathToRoot='" + fullPathToRoot + '\'' +
        ", currentDirectory='" + currentDirectory + '\'' +
        ", slaveHostname='" + slaveHostname + '\'' +
        '}';
  }
}
