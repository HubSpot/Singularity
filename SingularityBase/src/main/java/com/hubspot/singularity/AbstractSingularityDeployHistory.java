package com.hubspot.singularity;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityDeployHistory implements Comparable<SingularityDeployHistory>, SingularityHistoryItem {
  @Override
  public int compareTo(SingularityDeployHistory o) {
    return o.getDeployMarker().compareTo(getDeployMarker());
  }

  public abstract Optional<SingularityDeployResult> getDeployResult();

  public abstract SingularityDeployMarker getDeployMarker();

  public abstract Optional<SingularityDeploy> getDeploy();

  @Override
  @JsonIgnore
  public long getCreateTimestampForCalculatingHistoryAge() {
    return getDeployMarker().getTimestamp();
  }

  public abstract Optional<SingularityDeployStatistics> getDeployStatistics();
}
