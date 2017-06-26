package com.hubspot.singularity.expiring;

import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityExpiringRequestParent;

public abstract class SingularityExpiringParent<T extends SingularityExpiringRequestParent> {

  public abstract T getExpiringAPIRequestObject();

  public abstract Optional<String> getUser();

  public abstract long getStartMillis();

  public abstract String getActionId();
}
