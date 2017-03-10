package com.hubspot.singularity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityDisasterDataPoints {
  private final List<SingularityDisasterDataPoint> dataPoints;

  @JsonCreator
  public SingularityDisasterDataPoints(@JsonProperty("dataPoints") List<SingularityDisasterDataPoint> dataPoints) {
    this.dataPoints = dataPoints;
  }

  public static SingularityDisasterDataPoints empty() {
    return new SingularityDisasterDataPoints(new ArrayList<SingularityDisasterDataPoint>());
  }

  public List<SingularityDisasterDataPoint> getDataPoints() {
    return dataPoints;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityDisasterDataPoints that = (SingularityDisasterDataPoints) o;
    return Objects.equals(dataPoints, that.dataPoints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dataPoints);
  }

  @Override
  public String toString() {
    return "SingularityDisasterDataPoints{" +
        "dataPoints=" + dataPoints +
        '}';
  }
}
