package com.hubspot.singularity;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityDeployMarker implements Comparable<SingularityDeployMarker> {
  @Override
  public int compareTo(SingularityDeployMarker o) {
    return ComparisonChain.start()
        .compare(getTimestamp(), o.getTimestamp())
        .compare(getDeployId(), o.getDeployId())
        .result();
  }

  public abstract String getRequestId();

  public abstract String getDeployId();

  @Auxiliary
  public abstract long getTimestamp();

  @Auxiliary
  public abstract Optional<String> getUser();

  @Auxiliary
  public abstract Optional<String> getMessage();

}
