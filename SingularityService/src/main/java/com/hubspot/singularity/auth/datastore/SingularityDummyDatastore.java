package com.hubspot.singularity.auth.datastore;

import java.util.Collections;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.api.auth.SingularityUser;

@Singleton
public class SingularityDummyDatastore implements SingularityAuthDatastore {
  @Inject
  public SingularityDummyDatastore() {
  }

  @Override
  public Optional<SingularityUser> getUser(String username) {
    return Optional.of(new SingularityUser(username, Optional.of(username), Optional.of(username), Collections.emptySet()));
  }

  @Override
  public Optional<Boolean> isHealthy() {
    return Optional.of(true);
  }
}
