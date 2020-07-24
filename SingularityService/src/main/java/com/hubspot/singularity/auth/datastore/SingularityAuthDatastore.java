package com.hubspot.singularity.auth.datastore;

import com.hubspot.singularity.SingularityUser;
import java.util.Optional;

public interface SingularityAuthDatastore {
  Optional<SingularityUser> getUser(String username);
  Optional<Boolean> isHealthy();
}
