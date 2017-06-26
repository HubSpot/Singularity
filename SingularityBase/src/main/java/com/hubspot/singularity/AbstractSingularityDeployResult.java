package com.hubspot.singularity;

import java.util.List;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityDeployResult {

 /* SingularityDeployResult(DeployState deployState, SingularityLoadBalancerUpdate lbUpdate, List<SingularityDeployFailure> deployFailures) {
    this(deployState, Optional.of(String.format("Load balancer had state %s%s", lbUpdate.getLoadBalancerState(),
      lbUpdate.getMessage().isPresent() && lbUpdate.getMessage().get().length() > 0 ? String.format(" (%s)", lbUpdate.getMessage().get()) : "")), Optional.of(lbUpdate), deployFailures, System.currentTimeMillis());
  }*/

 public static SingularityDeployResult fromLbFailures(DeployState deployState, SingularityLoadBalancerUpdate lbUpdate, List<SingularityDeployFailure> deployFailures) {
   return SingularityDeployResult.builder()
       .setDeployState(deployState)
       .setMessage(String.format("Load balancer had state %s%s",
           lbUpdate.getLoadBalancerState(), lbUpdate.getMessage().isPresent() && lbUpdate.getMessage().get().length() > 0 ?
               String.format(" (%s)", lbUpdate.getMessage().get()) : ""))
       .setLbUpdate(lbUpdate)
       .setDeployFailures(deployFailures)
       .build();
 }

  public static SingularityDeployResult of(DeployState state) {
   return SingularityDeployResult.builder().setDeployState(state).build();
  }

  public abstract DeployState getDeployState();

  public abstract Optional<String> getMessage();

  public abstract Optional<SingularityLoadBalancerUpdate> getLbUpdate();

  public abstract List<SingularityDeployFailure> getDeployFailures();

  @Default
  public long getTimestamp() {
    return System.currentTimeMillis();
  }
}
