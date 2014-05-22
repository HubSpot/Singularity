package com.hubspot.singularity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityLoadBalancerService extends SingularityJsonObject {

  private final String serviceId;
  private final Collection<String> owners;
  private final String serviceBasePath;
  private final List<String> loadBalancerGroups;
  private final Optional<Map<String, Object>> options;

  public static SingularityLoadBalancerService fromRequestAndDeploy(SingularityRequest request, SingularityDeploy deploy) {
    final List<String> owners = request.getOwners().or(Collections.<String>emptyList());
    final List<String> loadBalancerGroups = deploy.getLoadBalancerGroups().or(Collections.<String>emptyList());

    return new SingularityLoadBalancerService(request.getId(), owners, deploy.getServiceBasePath().get(), loadBalancerGroups, deploy.getLoadBalancerOptions());
  }
  
  public static SingularityLoadBalancerService fromTaskRequest(SingularityTaskRequest taskRequest) {
    return fromRequestAndDeploy(taskRequest.getRequest(), taskRequest.getDeploy());
  }
  
  @JsonCreator
  public SingularityLoadBalancerService(@JsonProperty("serviceId") String serviceId,
                                        @JsonProperty("owners") Collection<String> owners,
                                        @JsonProperty("serviceBasePath") String serviceBasePath,
                                        @JsonProperty("loadBalancerGroups") List<String> loadBalancerGroups,
                                        @JsonProperty("options") Optional<Map<String, Object>> options) {
    this.serviceId = serviceId;
    this.owners = owners;
    this.serviceBasePath = serviceBasePath;
    this.loadBalancerGroups = loadBalancerGroups;
    this.options = options;
  }

  public String getServiceId() {
    return serviceId;
  }

  public Collection<String> getOwners() {
    return owners;
  }

  public String getServiceBasePath() {
    return serviceBasePath;
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
        ", serviceBasePath='" + serviceBasePath + '\'' +
        ", loadBalancerGroups=" + loadBalancerGroups +
        ", options=" + options +
        ']';
  }
}
