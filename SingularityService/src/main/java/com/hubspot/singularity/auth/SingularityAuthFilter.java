package com.hubspot.singularity.auth;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUser;

import io.dropwizard.auth.AuthFilter;

public class SingularityAuthFilter extends AuthFilter<ContainerRequestContext, SingularityUser> {

  @Inject
  public SingularityAuthFilter() {

  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {

  }
}
