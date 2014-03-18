package com.hubspot.singularity;


public class SingularityTaskIdHolder extends SingularityJsonObject {

  private final SingularityTaskId taskId;

  public SingularityTaskIdHolder(SingularityTaskId taskId) {
    this.taskId = taskId;
  }

  public SingularityTaskId getTaskId() {
    return taskId;
  }
  
}
