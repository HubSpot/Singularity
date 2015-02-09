package com.hubspot.singularity.smtp;

/**
 * POJO for Jade to generate task history tables in emails.
 */
public class SingularityMailTaskHistoryUpdate {
  private final String date;
  private final String update;
  private final String message;

  public SingularityMailTaskHistoryUpdate(String date, String update, String message) {
    this.date = date;
    this.update = update;
    this.message = message;
  }

  public String getDate() {
    return date;
  }

  public String getUpdate() {
    return update;
  }

  public String getMessage() {
    return message;
  }
}
