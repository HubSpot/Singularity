package com.hubspot.singularity.logwatcher.logrotate;

import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration;

public class LogrotateTemplateContext {

  private final String logfile;
  private final SingularityLogWatcherConfiguration configuration;
  
  public LogrotateTemplateContext(SingularityLogWatcherConfiguration configuration, String logfile) {
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
  
  public String getLogfile() {
    return logfile;
  }
  
}
