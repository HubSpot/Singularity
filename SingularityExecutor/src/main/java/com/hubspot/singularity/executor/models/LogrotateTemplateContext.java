package com.hubspot.singularity.executor.models;

import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskDefinition;

public class LogrotateTemplateContext {

  private final SingularityExecutorTaskDefinition taskDefinition;
  private final SingularityExecutorConfiguration configuration;
  
  public LogrotateTemplateContext(SingularityExecutorConfiguration configuration, SingularityExecutorTaskDefinition taskDefinition) {
    this.configuration = configuration;
    this.taskDefinition = taskDefinition;
  }
  
  public String getRotateDateformat() {
    return configuration.getLogrotateDateformat();
  }
  
  public String getRotateCount() {
    return configuration.getLogrotateCount();
  }
  
  public String getMaxageDays() {
    return configuration.getLogrotateMaxageDays();
  }
  
  public String getRotateDirectory() {
    return configuration.getLogrotateToDirectory();
  }
  
  public String[] getExtrasFiles() {
    final String[] original = configuration.getLogrotateExtrasFiles();
    final String[] transformed = new String[original.length];
    
    for (int i = 0; i < original.length; i++) {
      transformed[i] = taskDefinition.getTaskDirectoryPath().resolve(original[i]).toString();
    }
    
    return transformed;
  }
  
  public String getExtrasDateformat() {
    return configuration.getLogrotateExtrasDateformat();
  }
  
  public String getLogfile() {
    return taskDefinition.getServiceLogOut();
  }

  @Override
  public String toString() {
    return "LogrotateTemplateContext [taskId=" + taskDefinition.getTaskId() + "]";
  }

  
}
