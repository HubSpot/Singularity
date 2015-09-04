package com.hubspot.singularity.auth.datastore;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityUser;

public interface SingularityAuthDatastore {
  Optional<SingularityUser> getUser(String username);
  Optional<Boolean> isHealthy();
}
