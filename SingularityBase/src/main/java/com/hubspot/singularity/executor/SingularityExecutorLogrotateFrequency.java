package com.hubspot.singularity.executor;

public enum SingularityExecutorLogrotateFrequency {
    HOURLY("monthly"),
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly");

    private final String logrotateFrequencyValue;

    SingularityExecutorLogrotateFrequency(String logrotateFrequencyValue) {
        this.logrotateFrequencyValue = logrotateFrequencyValue;
    }

    public String getLogrotateFrequencyValue() {
        return logrotateFrequencyValue;
    }
}

