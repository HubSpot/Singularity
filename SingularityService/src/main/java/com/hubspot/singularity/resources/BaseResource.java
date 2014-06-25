package com.hubspot.singularity.resources;

import com.hubspot.singularity.auth.SingularityUser;
import io.dropwizard.auth.Auth;

public abstract class BaseResource {
  @Auth
  protected SingularityUser user;
}
