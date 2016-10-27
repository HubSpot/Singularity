package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class SingularityDisastersData {
  private final List<SingularityDisasterDataPoint> stats;
  private final List<SingularityDisaster> disasters;
  private final boolean automatedActionDisabled;

  @JsonCreator
  public SingularityDisastersData(@JsonProperty("stats") List<SingularityDisasterDataPoint> stats,
                                  @JsonProperty("disasterStates") List<SingularityDisaster> disasters,
                                  @JsonProperty("automatedActionDisabled") boolean automatedActionDisabled) {
    this.stats = stats;
    this.disasters = disasters;
    this.automatedActionDisabled = automatedActionDisabled;
  }

  public List<SingularityDisasterDataPoint> getStats() {
    return stats;
  }

  public List<SingularityDisaster> getDisasters() {
    return disasters;
  }

  public boolean isAutomatedActionsDisabled() {
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
      Objects.equal(stats, that.stats) &&
      Objects.equal(disasters, that.disasters);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(stats, disasters, automatedActionDisabled);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("stats", stats)
      .add("disasters", disasters)
      .add("automatedActionDisabled", automatedActionDisabled)
      .toString();
  }
}
