package com.hubspot.singularity.executor.models;

import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;

public class LogrotateTemplateContext {

  private final String logfile;
  private final SingularityExecutorConfiguration configuration;
  
  public LogrotateTemplateContext(SingularityExecutorConfiguration configuration, String logfile) {
    this.configuration = configuration;
    this.logfile = logfile;
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
    return configuration.getLogrotateExtrasFiles();
  }
  
  public String getExtrasDateformat() {
    return configuration.getLogrotateExtrasDateformat();
  }
  
  public String getLogfile() {
    return logfile;
  }

  @Override
  public String toString() {
    return "LogrotateTemplateContext [logfile=" + logfile + ", configuration=" + configuration + "]";
  }
  
}
