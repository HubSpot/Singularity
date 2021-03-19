package com.hubspot.singularity.executor;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;

public enum SingularityExecutorLogrotateFrequency {
  EVERY_MINUTE("daily", Optional.of("* * * * *")),
  EVERY_FIVE_MINUTES("daily", Optional.of("*/5 * * * *")),
  HOURLY("daily", Optional.of("0 * * * *")), // we have to use the "daily" frequency because not all versions of logrotate support "hourly"
  DAILY("daily", Optional.empty()),
  WEEKLY("weekly", Optional.empty()),
  MONTHLY("monthly", Optional.empty());

  private final String logrotateValue;
  private final Optional<String> cronSchedule;

  public static final Set<SingularityExecutorLogrotateFrequency> HOURLY_OR_MORE_FREQUENT_LOGROTATE_VALUES = ImmutableSet.of(
    EVERY_MINUTE,
    EVERY_FIVE_MINUTES,
    HOURLY
  );

  SingularityExecutorLogrotateFrequency(
    String logrotateValue,
    Optional<String> cronSchedule
  ) {
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
