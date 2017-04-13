package com.hubspot.singularity.smtp;

/**
 * POJO for Jade to generate task metadata tables in emails.
 */
public class SingularityMailTaskMetadata {
    private final String date;
    private final String type;
    private final String title;
    private final String user;
    private final String message;
    private final String level;

    public SingularityMailTaskMetadata(String date, String type, String title, String user, String message, String level) {
        this.date = date;
        this.type = type;
        this.title = title;
        this.user = user;
        this.message = message;
        this.level = level;
    }

    public String getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public String getTitle() { return title; }

    public String getUser() { return user; }

    public String getMessage() {
        return message;
    }

    public String getLevel() {
        return level;
    }
}
