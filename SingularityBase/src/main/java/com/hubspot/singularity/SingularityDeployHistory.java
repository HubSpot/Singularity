package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityDeployHistory extends SingularityJsonObject implements Comparable<SingularityDeployHistory> {
  
  private final Optional<SingularityDeployResult> deployResult;
  private final SingularityDeployMarker deployMarker;
  private final Optional<SingularityDeploy> deploy;
  private final Optional<SingularityDeployStatistics> deployStatistics;
  
  public static SingularityDeployHistory fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityDeployHistory.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }
  
  @JsonCreator
  public SingularityDeployHistory(@JsonProperty("deployResult") Optional<SingularityDeployResult> deployResult, @JsonProperty("deployMarker") SingularityDeployMarker deployMarker, 
      @JsonProperty("deploy") Optional<SingularityDeploy> deploy, @JsonProperty("deployStatistics") Optional<SingularityDeployStatistics> deployStatistics) {
    this.deployResult = deployResult;
    this.deployMarker = deployMarker;
    this.deploy = deploy;
    this.deployStatistics = deployStatistics;
  }
  
  @Override
  public int compareTo(SingularityDeployHistory o) {
    return o.getDeployMarker().compareTo(getDeployMarker());
  }

  public Optional<SingularityDeployResult> getDeployResult() {
    return deployResult;
  }

  public SingularityDeployMarker getDeployMarker() {
    return deployMarker;
  }

  public Optional<SingularityDeploy> getDeploy() {
    return deploy;
  }

  public Optional<SingularityDeployStatistics> getDeployStatistics() {
    return deployStatistics;
  }

  @Override
  public String toString() {
    return "SingularityDeployHistory [deployResult=" + deployResult + ", deployMarker=" + deployMarker + ", deploy=" + deploy + ", deployStatistics=" + deployStatistics + "]";
  }
  
}
