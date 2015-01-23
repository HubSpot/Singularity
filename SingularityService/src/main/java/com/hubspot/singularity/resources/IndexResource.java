package com.hubspot.singularity.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.views.IndexView;

@Path("/{wildcard:.*}")
@Produces(MediaType.TEXT_HTML)
public class IndexResource {
  private final SingularityConfiguration configuration;
  private final ObjectMapper objectMapper;

  @Inject
  public IndexResource(SingularityConfiguration configuration, ObjectMapper objectMapper) {
    this.configuration = configuration;
    this.objectMapper = objectMapper;
  }

  @GET
  public IndexView getIndex() {
    return new IndexView(configuration, objectMapper);
  }
}
