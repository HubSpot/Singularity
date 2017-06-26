package com.hubspot.singularity.expiring;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.api.SingularitySkipHealthchecksRequest;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityExpiringSkipHealthchecks extends SingularityExpiringRequestActionParent<SingularitySkipHealthchecksRequest> {

  public abstract String getRequestId();

  public abstract Optional<String> getUser();

  public abstract long getStartMillis();

  public abstract SingularitySkipHealthchecksRequest getExpiringAPIRequestObject();

  public abstract Optional<Boolean> getRevertToSkipHealthchecks();

  public abstract String getActionId();
}
