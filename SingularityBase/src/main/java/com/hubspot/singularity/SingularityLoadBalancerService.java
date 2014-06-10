package com.hubspot.singularity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.baragon.models.BaragonService;

public class SingularityLoadBalancerService extends BaragonService {
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
    super(serviceId, owners, serviceBasePath, loadBalancerGroups, options.orNull());
  }
}
