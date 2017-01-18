package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.data.UsageManager;
import com.wordnik.swagger.annotations.Api;

@Path(UsageResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Provides usage data about slaves and tasks", value=UsageResource.PATH)
public class UsageResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/usage";

  private final UsageManager usageManager;

  @Inject
  public UsageResource(UsageManager usageManager) {
    this.usageManager = usageManager;
  }

  @Path("/slaves")
  public List<SingularitySlaveUsageWithId> getSlavesWithUsage() {
    return usageManager.getAllCurrentSlaveUsage();
  }

}
