package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.util.Objects;
import java.util.Set;

public class DeployProgressLbUpdateHolder {
  private final SingularityLoadBalancerUpdate loadBalancerUpdate;
  private final Set<SingularityTaskId> added;
  private final Set<SingularityTaskId> removed;

  @JsonCreator
  public DeployProgressLbUpdateHolder(
    @JsonProperty("loadBalancerUpdate") SingularityLoadBalancerUpdate loadBalancerUpdate,
    @JsonProperty("added") Set<SingularityTaskId> added,
    @JsonProperty("removed") Set<SingularityTaskId> removed
  ) {
    this.loadBalancerUpdate = loadBalancerUpdate;
    this.added = added;
    this.removed = removed;
  }

  public SingularityLoadBalancerUpdate getLoadBalancerUpdate() {
    return loadBalancerUpdate;
  }

  public Set<SingularityTaskId> getAdded() {
    return added;
  }

  public Set<SingularityTaskId> getRemoved() {
    return removed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeployProgressLbUpdateHolder that = (DeployProgressLbUpdateHolder) o;
    return (
      Objects.equals(loadBalancerUpdate, that.loadBalancerUpdate) &&
      Objects.equals(added, that.added) &&
      Objects.equals(removed, that.removed)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(loadBalancerUpdate, added, removed);
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("loadBalancerUpdate", loadBalancerUpdate)
      .add("added", added)
      .add("removed", removed)
      .toString();
  }
}
