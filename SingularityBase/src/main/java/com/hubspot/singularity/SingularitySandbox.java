package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.json.MesosFileObject;

public class SingularitySandbox extends SingularityJsonObject {

  private final List<MesosFileObject> files;
  private final String rootPath;

  @JsonCreator
  public SingularitySandbox(@JsonProperty("files") List<MesosFileObject> files, @JsonProperty("rootPath") String rootPath) {
    this.files = files;
    this.rootPath = rootPath;
  }

  public List<MesosFileObject> getFiles() {
    return files;
  }

  public String getRootPath() {
    return rootPath;
  }

  @Override
  public String toString() {
    return "SingularitySandbox [files=" + files + ", rootPath=" + rootPath + "]";
  }


}
