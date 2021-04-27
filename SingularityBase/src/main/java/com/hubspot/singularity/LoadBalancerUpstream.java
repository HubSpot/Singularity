package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.hubspot.baragon.models.UpstreamInfo;
import java.util.Objects;
import java.util.Optional;

public class LoadBalancerUpstream {
  private final String upstream;
  private final String group;
  private final Optional<String> rackId;

  @JsonCreator
  public LoadBalancerUpstream(
    @JsonProperty("upstream") String upstream,
    @JsonProperty("group") String group,
    @JsonProperty("rackId") Optional<String> rackId
  ) {
    this.upstream = upstream;
    this.group = group;
    this.rackId = rackId;
  }

  public static LoadBalancerUpstream fromBaragonUpstream(UpstreamInfo upstreamInfo) {
    return new LoadBalancerUpstream(
      upstreamInfo.getUpstream(),
      upstreamInfo.getGroup(),
      upstreamInfo.getRackId().toJavaUtil()
    );
  }

  public String getUpstream() {
    return upstream;
  }

  public String getGroup() {
    return group;
  }

  public Optional<String> getRackId() {
    return rackId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LoadBalancerUpstream that = (LoadBalancerUpstream) o;
    return (
      Objects.equals(upstream, that.upstream) &&
      Objects.equals(group, that.group) &&
      Objects.equals(rackId, that.rackId)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(upstream, group, rackId);
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("upstream", upstream)
      .add("group", group)
      .add("rackId", rackId)
      .toString();
  }
}
