package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Represents the path to a specific task's Mesos sandbox")
public class SingularitySandbox {
  private final List<SingularitySandboxFile> files;
  private final String fullPathToRoot;
  private final String currentDirectory;
  private final String agentHostname;

  @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_NONVIRTUAL")
  public SingularitySandbox(
    List<SingularitySandboxFile> files,
    String fullPathToRoot,
    String currentDirectory,
    String agentHostname
  ) {
    this(files, fullPathToRoot, currentDirectory, null, agentHostname);
  }

  @JsonCreator
  public SingularitySandbox(
    @JsonProperty("files") List<SingularitySandboxFile> files,
    @JsonProperty("fullPathToRoot") String fullPathToRoot,
    @JsonProperty("currentDirectory") String currentDirectory,
    @JsonProperty("slaveHostname") String slaveHostname,
    @JsonProperty("agentHostname") String agentHostname
  ) {
    this.files = files;
    this.currentDirectory = currentDirectory;
    this.fullPathToRoot = fullPathToRoot;
    this.agentHostname = agentHostname != null ? agentHostname : slaveHostname;
  }

  @Schema(description = "Full path to the root of the Mesos task sandbox")
  public String getFullPathToRoot() {
    return fullPathToRoot;
  }

  @Schema(description = "Hostname of tasks's slave")
  public String getAgentHostname() {
    return agentHostname;
  }

  @Schema(description = "Hostname of tasks's slave")
  @Deprecated
  public String getSlaveHostname() {
    return agentHostname;
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
    return (
      "SingularitySandbox{" +
      "files=" +
      files +
      ", fullPathToRoot='" +
      fullPathToRoot +
      '\'' +
      ", currentDirectory='" +
      currentDirectory +
      '\'' +
      ", agentHostname='" +
      agentHostname +
      '\'' +
      '}'
    );
  }
}
