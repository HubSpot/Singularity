package com.hubspot.singularity.resources;

import com.google.inject.Inject;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.SingularityConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path(ApiPaths.CONFIGURATION_RESOURCE_PATH)
@Schema(title = "Exposes some Singularity configuration values")
public class SingularityConfigurationResource {
  private final SingularityConfiguration config;

  @Inject
  public SingularityConfigurationResource(SingularityConfiguration config) {
    this.config = config;
  }

  @GET
  @Path("/agent/max-decommissioning-count")
  @Operation(summary = "Retrieve the max decommissioning agent count from configuration")
  public int getMaxDecommissioningAgent() {
    return config.getMaxDecommissioningAgents();
  }
}
