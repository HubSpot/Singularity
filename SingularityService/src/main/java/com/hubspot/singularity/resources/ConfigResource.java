package com.hubspot.singularity.resources;

import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.views.ConfigView;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/config.js")
@Produces("application/javascript")
public class ConfigResource {
  private final SingularityConfiguration configuration;

  @Inject
  public ConfigResource(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  @GET
  public ConfigView getConstants() {
    return new ConfigView(configuration);
  }
}
