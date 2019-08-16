package com.hubspot.singularity.auth.datastore;

import java.util.Optional;

import com.hubspot.singularity.SingularityUser;

public interface SingularityAuthDatastore {
  Optional<SingularityUser> getUser(String username);
  Optional<Boolean> isHealthy();
}
