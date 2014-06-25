package com.hubspot.singularity.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.auth.SingularityUser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.views.IndexView;
import io.dropwizard.auth.Auth;

@Path("/{wildcard:.*}")
@Produces(MediaType.TEXT_HTML)
public class IndexResource {
  private final SingularityConfiguration configuration;

  @Auth
  SingularityUser user;

  @Inject
  public IndexResource(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  @GET
  public IndexView getIndex() {
    return new IndexView(configuration, user);
  }
}
