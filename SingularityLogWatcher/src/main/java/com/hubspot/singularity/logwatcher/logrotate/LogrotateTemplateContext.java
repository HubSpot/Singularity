package com.hubspot.singularity.logwatcher.logrotate;

public class LogrotateTemplateContext {

  private final String olddir;
  private final String logfile;
  
  public LogrotateTemplateContext(String olddir, String logfile) {
    this.olddir = olddir;
    this.logfile = logfile;
  }
  
  public String getOlddir() {
    return olddir;
  }
  
  public String getLogfile() {
    return logfile;
  }
  
  @Override
  public String toString() {
    return "LogrotateTemplateContext [olddir=" + olddir + ", logfile=" + logfile + "]";
  }
  
}
