package com.hubspot.singularity;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties (ignoreUnknown = true)
public class SingularityRequestLbCleanup {
  private String requestId;
  private Set<String> loadBalancerGroups;
  private String serviceBasePath;
  private List<String> activeTaskIds;
  private Optional<SingularityLoadBalancerUpdate> loadBalancerUpdate;

  public SingularityRequestLbCleanup(@JsonProperty("requestId") String requestId,
                                     @JsonProperty("loadBalancerGroups") Set<String> loadBalancerGroups,
                                     @JsonProperty("serviceBasePath") String serviceBasePath,
                                     @JsonProperty("activeTaskIds") List<String> activeTaskIds,
                                     @JsonProperty("loadBalancerUpdate") Optional<SingularityLoadBalancerUpdate> loadBalancerUpdate) {
    this.requestId = requestId;
    this.loadBalancerGroups = loadBalancerGroups;
    this.serviceBasePath = serviceBasePath;
    this.activeTaskIds = activeTaskIds;
    this.loadBalancerUpdate = loadBalancerUpdate;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public Set<String> getLoadBalancerGroups() {
    return loadBalancerGroups;
  }

  public void setLoadBalancerGroups(Set<String> loadBalancerGroups) {
    this.loadBalancerGroups = loadBalancerGroups;
  }

  public String getServiceBasePath() {
    return serviceBasePath;
  }

  public void setServiceBasePath(String serviceBasePath) {
    this.serviceBasePath = serviceBasePath;
  }

  public List<String> getActiveTaskIds() {
    return activeTaskIds;
  }

  public void setActiveTaskIds(List<String> activeTaskIds) {
    this.activeTaskIds = activeTaskIds;
  }

  public Optional<SingularityLoadBalancerUpdate> getLoadBalancerUpdate() {
    return loadBalancerUpdate;
  }

  public void setLoadBalancerUpdate(Optional<SingularityLoadBalancerUpdate> loadBalancerUpdate) {
    this.loadBalancerUpdate = loadBalancerUpdate;
  }

  @Override
  public String toString() {
    return "SingularityRequestLbCleanup{" +
        "requestId='" + requestId + '\'' +
        ", loadBalancerGroups=" + loadBalancerGroups +
        ", serviceBasePath='" + serviceBasePath + '\'' +
        ", activeTaskIds=" + activeTaskIds +
        ", loadBalancerUpdate=" + loadBalancerUpdate +
        '}';
  }
}
