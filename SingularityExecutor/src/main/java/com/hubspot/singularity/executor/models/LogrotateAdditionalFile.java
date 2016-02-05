package com.hubspot.singularity.executor.models;

public class LogrotateAdditionalFile {
    private final String filename;
    private final String extension;
    private final String dateformat;

    public LogrotateAdditionalFile(String filename, String extension, String dateformat) {
        this.filename = filename;
        this.extension = extension;
        this.dateformat = dateformat;
    }

    public String getFilename() {
        return filename;
    }

    public String getExtension() {
        return extension;
    }

    public String getDateformat() {
        return dateformat;
    }

    @Override
    public String toString() {
        return "LogrotateAdditionalFile[" +
            "filename='" + filename + '\'' +
            ", extension='" + extension + '\'' +
            ", dateformat='" + dateformat + '\'' +
            ']';
    }
}
