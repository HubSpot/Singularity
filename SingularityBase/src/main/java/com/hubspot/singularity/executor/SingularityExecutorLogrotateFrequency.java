package com.hubspot.singularity.executor;

import com.google.common.base.Optional;

public enum SingularityExecutorLogrotateFrequency {
    HOURLY("hourly", Optional.of("0 * * * *")),
    DAILY("daily", Optional.<String>absent()),
    WEEKLY("weekly", Optional.<String>absent()),
    MONTHLY("monthly", Optional.<String>absent());

    private final String logrotateFrequencyValue;
    private final Optional<String> cronSchedule;

    SingularityExecutorLogrotateFrequency(String logrotateFrequencyValue, Optional<String> cronSchedule) {
        this.logrotateFrequencyValue = logrotateFrequencyValue;
        this.cronSchedule = cronSchedule;
    }

    public String getLogrotateFrequencyValue() {
        return logrotateFrequencyValue;
    }

    public Optional<String> getCronSchedule() {
        return cronSchedule;
    }
}

