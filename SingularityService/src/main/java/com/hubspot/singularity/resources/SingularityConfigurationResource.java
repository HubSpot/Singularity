package com.hubspot.singularity.resources;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityLimits;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.SingularityConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ApiPaths.CONFIGURATION_RESOURCE_PATH)
@Schema(title = "Exposes some Singularity configuration values")
public class SingularityConfigurationResource {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityConfigurationResource.class
  );
  private final SingularityConfiguration config;

  @Inject
  public SingularityConfigurationResource(SingularityConfiguration config) {
    this.config = config;
  }

  @GET
  @Path("/singularity-limits")
  @Operation(summary = "Retrieve configuration data for Singularity")
  public SingularityLimits getSingularityLimits() {
    SingularityLimits limits = new SingularityLimits(
      config.getMaxDecommissioningAgents()
    );
    LOG.info(
      "Getting singularity limits, max decom: " + limits.getMaxDecommissioningAgents()
    );
    return new SingularityLimits(config.getMaxDecommissioningAgents());
  }
}
