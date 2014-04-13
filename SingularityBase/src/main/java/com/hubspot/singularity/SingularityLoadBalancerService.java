package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SingularityLoadBalancerService extends SingularityJsonObject {
  private final String serviceId;
  private final Collection<String> owners;
  private final String loadBalancerBaseUri;
  private final List<String> loadBalancerGroups;
  private final Optional<Map<String, Object>> options;

  @JsonCreator
  public SingularityLoadBalancerService(@JsonProperty("serviceId") String serviceId,
                                        @JsonProperty("owners") Collection<String> owners,
                                        @JsonProperty("loadBalancerBaseUri") String loadBalancerBaseUri,
                                        @JsonProperty("loadBalancerGroups") List<String> loadBalancerGroups,
                                        @JsonProperty("options") Optional<Map<String, Object>> options) {
    this.serviceId = serviceId;
    this.owners = owners;
    this.loadBalancerBaseUri = loadBalancerBaseUri;
    this.loadBalancerGroups = loadBalancerGroups;
    this.options = options;
  }

  public String getServiceId() {
    return serviceId;
  }

  public Collection<String> getOwners() {
    return owners;
  }

  public String getLoadBalancerBaseUri() {
    return loadBalancerBaseUri;
  }

  public List<String> getLoadBalancerGroups() {
    return loadBalancerGroups;
  }

  public Optional<Map<String, Object>> getOptions() {
    return options;
  }

  @Override
  public String toString() {
    return "SingularityLoadBalancerService [" +
        "serviceId='" + serviceId + '\'' +
        ", owners=" + owners +
        ", loadBalancerBaseUri='" + loadBalancerBaseUri + '\'' +
        ", loadBalancerGroups=" + loadBalancerGroups +
        ", options=" + options +
        ']';
  }

  public static Optional<SingularityLoadBalancerService> fromTaskRequest(SingularityTaskRequest taskRequest) {
    final List<String> owners = taskRequest.getRequest().getOwners().or(Collections.<String>emptyList());
    final List<String> loadBalancerGroups = taskRequest.getDeploy().getLoadBalancerGroups().or(Collections.<String>emptyList());

    if (!taskRequest.getRequest().isLoadBalanced() || !taskRequest.getDeploy().getLoadBalancerBaseUri().isPresent()) {
      return Optional.absent();
    }

    return Optional.of(new SingularityLoadBalancerService(taskRequest.getRequest().getId(), owners, taskRequest.getDeploy().getLoadBalancerBaseUri().get(), loadBalancerGroups, Optional.<Map<String, Object>>absent()));
  }
}
