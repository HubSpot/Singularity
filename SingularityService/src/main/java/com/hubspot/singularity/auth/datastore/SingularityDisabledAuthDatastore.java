package com.hubspot.singularity.auth.datastore;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;

@Singleton
public class SingularityDisabledAuthDatastore implements SingularityAuthDatastore {
  @Inject
  public SingularityDisabledAuthDatastore() {}

  @Override
  public Optional<SingularityUser> getUser(String username) {
    return Optional.absent();
  }

  @Override
  public Optional<Boolean> isHealthy() {
    return Optional.absent();
  }
}
