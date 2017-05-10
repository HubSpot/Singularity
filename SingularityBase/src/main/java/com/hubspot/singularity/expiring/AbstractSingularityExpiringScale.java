package com.hubspot.singularity.expiring;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.api.SingularityScaleRequest;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityExpiringScale.class)
public abstract class AbstractSingularityExpiringScale extends SingularityExpiringRequestActionParent<SingularityScaleRequest> {

  public abstract String getRequestId();

  public abstract Optional<String> getUser();

  public abstract long getStartMillis();

  public abstract SingularityScaleRequest getExpiringAPIRequestObject();

  public abstract Optional<Integer> getRevertToInstances();

  public abstract String getActionId();

  public abstract Optional<Boolean> getBounce();

}
