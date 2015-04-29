package com.hubspot.singularity.smtp;

/**
 * Emails contain logs for tasks, this object is easily interpretable by Jade
 * to generate the log entries.
 */
public class SingularityMailTaskLog {
  private final String path;
  private final String file;
  private final String link;
  private final String log;

  public SingularityMailTaskLog(String path, String file, String link, String log) {
    this.path = path;
    this.file = file;
    this.link = link;
    this.log = log;
  }

  public String getPath() {
    return path;
  }

  public String getFile() {
    return file;
  }

  public String getLink() {
    return link;
  }

  public String getLog() {
    return log;
  }

  @Override
  public String toString() {
    return "SingularityMailTaskLog [path=" + path +
        " file=" + file + " link=" + link + " log=" + log + "]";
  }
}
