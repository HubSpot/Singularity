package com.hubspot.singularity.executor;

import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

public enum SingularityExecutorLogrotateFrequency {
  EVERY_MINUTE("daily", Optional.of("* * * * *"), 5),
  EVERY_FIVE_MINUTES("daily", Optional.of("*/5 * * * *"), 4),
  HOURLY("daily", Optional.of("0 * * * *"), 3), // we have to use the "daily" frequency because not all versions of logrotate support "hourly"
  DAILY("daily", Optional.empty(), 2),
  WEEKLY("weekly", Optional.empty(), 1),
  MONTHLY("monthly", Optional.empty(), 0);

  private final String logrotateValue;
  private final Optional<String> cronSchedule;
  private final int granularityRank;

  public static final Set<SingularityExecutorLogrotateFrequency> HOURLY_OR_MORE_FREQUENT_LOGROTATE_VALUES = ImmutableSet.of(
    EVERY_MINUTE,
    EVERY_FIVE_MINUTES,
    HOURLY
  );

  SingularityExecutorLogrotateFrequency(
    String logrotateValue,
    Optional<String> cronSchedule,
    int granularityRank
  ) {
    this.logrotateValue = logrotateValue;
    this.cronSchedule = cronSchedule;
    this.granularityRank = granularityRank;
  }

  public String getLogrotateValue() {
    return logrotateValue;
  }

  public Optional<String> getCronSchedule() {
    return cronSchedule;
  }

  public int getGranularityRank() {
    return granularityRank;
  }

  public static Comparator<Optional<SingularityExecutorLogrotateFrequency>> getComparator() {
    return Comparators.emptiesLast(
      Comparator
        .comparing(SingularityExecutorLogrotateFrequency::getGranularityRank)
        .reversed()
    );
  }
}
