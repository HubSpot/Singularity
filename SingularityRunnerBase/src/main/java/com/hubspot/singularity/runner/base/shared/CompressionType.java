package com.hubspot.singularity.runner.base.shared;

public enum CompressionType {
  GZIP("gzip", ".gz"), BZIP2("bzip2", ".bz2"), BGZIP("bgzip", ".gz");

  private final String command;
  private final String extention;

  CompressionType(String command, String extention) {
    this.command = command;
    this.extention = extention;
  }

  public String getCommand() {
    return command;
  }

  public String getExtention() {
    return extention;
  }
}
