package com.hubspot.singularity.auth.datastore;

import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.api.auth.SingularityUser;

@Singleton
public class SingularityDisabledAuthDatastore implements SingularityAuthDatastore {
  @Inject
  public SingularityDisabledAuthDatastore() {}

  @Override
  public Optional<SingularityUser> getUser(String username) {
    return Optional.empty();
  }

  @Override
  public Optional<Boolean> isHealthy() {
    return Optional.empty();
  }
}
