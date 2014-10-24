package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularitySandbox extends SingularityJsonObject {

  private final List<SingularitySandboxFile> files;
  private final String fullPathToRoot;
  private final String currentDirectory;
  private final String slaveHostname;

  @JsonCreator
  public SingularitySandbox(@JsonProperty("files") List<SingularitySandboxFile> files, @JsonProperty("fullPathToRoot") String fullPathToRoot, @JsonProperty("currentDirectory") String currentDirectory, @JsonProperty("slaveHostname") String slaveHostname) {
    this.files = files;
    this.currentDirectory = currentDirectory;
    this.fullPathToRoot = fullPathToRoot;
    this.slaveHostname = slaveHostname;
  }

  public String getFullPathToRoot() {
    return fullPathToRoot;
  }

  public String getSlaveHostname() {
    return slaveHostname;
  }

  public List<SingularitySandboxFile> getFiles() {
    return files;
  }

  public String getCurrentDirectory() {
    return currentDirectory;
  }

  @Override
  public String toString() {
    return "SingularitySandbox [files=" + files + ", fullPathToRoot=" + fullPathToRoot + ", currentDirectory=" + currentDirectory + ", slaveHostname=" + slaveHostname + "]";
  }


}
