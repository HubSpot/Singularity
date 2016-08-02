package com.hubspot.singularity.executor;

import com.google.common.base.Optional;

public enum SingularityExecutorLogrotateFrequency {
    HOURLY("daily", Optional.of("0 * * * *")),  // we have to use the "daily" frequency because not all versions of logrotate support "hourly"
    DAILY("daily", Optional.<String>absent()),
    WEEKLY("weekly", Optional.<String>absent()),
    MONTHLY("monthly", Optional.<String>absent());

    private final String logrotateValue;
    private final Optional<String> cronSchedule;

    SingularityExecutorLogrotateFrequency(String logrotateValue, Optional<String> cronSchedule) {
        this.logrotateValue = logrotateValue;
        this.cronSchedule = cronSchedule;
    }

    public String getLogrotateValue() {
        return logrotateValue;
    }

    public Optional<String> getCronSchedule() {
        return cronSchedule;
    }
}

