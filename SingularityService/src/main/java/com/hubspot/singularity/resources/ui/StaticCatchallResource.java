package com.hubspot.singularity.resources.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.config.IndexViewConfiguration;
import com.hubspot.singularity.resources.SingularityServiceUIModule;
import com.hubspot.singularity.views.IndexView;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/{uiPath:.*}")
@Produces(MediaType.TEXT_HTML)
@Consumes(MediaType.WILDCARD)
public class StaticCatchallResource {

  private final IndexViewConfiguration configuration;
  private final String singularityUriBase;
  private final ObjectMapper mapper;

  @Inject
  public StaticCatchallResource(
    @Named(SingularityServiceUIModule.SINGULARITY_URI_BASE) String singularityUriBase,
    IndexViewConfiguration configuration,
    @Singularity ObjectMapper mapper
  ) {
    this.configuration = configuration;
    this.singularityUriBase = singularityUriBase;
    this.mapper = mapper;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public IndexView getIndex() {
    return new IndexView(singularityUriBase, "", configuration, mapper);
  }
}
