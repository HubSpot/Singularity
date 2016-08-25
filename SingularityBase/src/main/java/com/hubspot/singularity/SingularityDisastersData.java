package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class SingularityDisastersData {
  private final Optional<SingularityDisasterStats> currentStats;
  private final Optional<SingularityDisasterStats> lastStats;
  private final List<SingularityDisasterType> activeDisasters;

  @JsonCreator
  public SingularityDisastersData(@JsonProperty("currentStats") Optional<SingularityDisasterStats> currentStats,
                                  @JsonProperty("lastStats") Optional<SingularityDisasterStats> lastStats,
                                  @JsonProperty("activeDisasters") List<SingularityDisasterType> activeDisasters) {
    this.currentStats = currentStats;
    this.lastStats = lastStats;
    this.activeDisasters = activeDisasters;
  }

  public Optional<SingularityDisasterStats> getCurrentStats() {
    return currentStats;
  }

  public Optional<SingularityDisasterStats> getLastStats() {
    return lastStats;
  }

  public List<SingularityDisasterType> getActiveDisasters() {
    return activeDisasters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityDisastersData that = (SingularityDisastersData) o;
    return Objects.equal(currentStats, that.currentStats) &&
      Objects.equal(lastStats, that.lastStats) &&
      Objects.equal(activeDisasters, that.activeDisasters);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(currentStats, lastStats, activeDisasters);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("currentStats", currentStats)
      .add("lastStats", lastStats)
      .add("activeDisasters", activeDisasters)
      .toString();
  }
}
