package com.hubspot.singularity;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class SingularityDisastersData {
  private final Optional<SingularityDisasterStats> currentStats;
  private final Optional<SingularityDisasterStats> lastStats;
  private final List<SingularityDisaster> disasters;
  private final boolean automatedActionDisabled;

  @JsonCreator
  public SingularityDisastersData(@JsonProperty("currentStats") Optional<SingularityDisasterStats> currentStats,
                                  @JsonProperty("lastStats") Optional<SingularityDisasterStats> lastStats,
                                  @JsonProperty("disasterStates") List<SingularityDisaster> disasters,
                                  @JsonProperty("automatedActionDisabled") boolean automatedActionDisabled) {
    this.currentStats = currentStats;
    this.lastStats = lastStats;
    this.disasters = disasters;
    this.automatedActionDisabled = automatedActionDisabled;
  }

  public Optional<SingularityDisasterStats> getCurrentStats() {
    return currentStats;
  }

  public Optional<SingularityDisasterStats> getLastStats() {
    return lastStats;
  }

  public List<SingularityDisaster> getDisasters() {
    return disasters;
  }

  public boolean isautomatedActionsDisabled() {
    return automatedActionDisabled;
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
    return automatedActionDisabled == that.automatedActionDisabled &&
      Objects.equal(currentStats, that.currentStats) &&
      Objects.equal(lastStats, that.lastStats) &&
      Objects.equal(disasters, that.disasters);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(currentStats, lastStats, disasters, automatedActionDisabled);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("currentStats", currentStats)
      .add("lastStats", lastStats)
      .add("disasters", disasters)
      .add("automatedActionDisabled", automatedActionDisabled)
      .toString();
  }
}
