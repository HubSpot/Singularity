package com.hubspot.singularity;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Information about currently active disasters")
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

  @Schema(description = "Previous data points for disaster metrics")
  public List<SingularityDisasterDataPoint> getStats() {
    return stats;
  }

  @Schema(description = "A list of active disasters")
  public List<SingularityDisaster> getDisasters() {
    return disasters;
  }

  @Schema(description = "`true` if automated disaster actions are currently disabled")
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
        Objects.equals(stats, that.stats) &&
        Objects.equals(disasters, that.disasters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stats, disasters, automatedActionDisabled);
  }

  @Override
  public String toString() {
    return "SingularityDisastersData{" +
        "stats=" + stats +
        ", disasters=" + disasters +
        ", automatedActionDisabled=" + automatedActionDisabled +
        '}';
  }
}
