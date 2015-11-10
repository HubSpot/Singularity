package com.hubspot.singularity.resources;

import static com.hubspot.singularity.SingularityMainModule.SINGULARITY_URI_BASE;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.views.IndexView;

@Singleton
@Path("/{uiPath:.*}")
public class StaticCatchallResource {
  private final SingularityConfiguration configuration;
  private final String singularityUriBase;
  private final ObjectMapper mapper;

  @Inject
  public StaticCatchallResource(@Named(SINGULARITY_URI_BASE) String singularityUriBase, SingularityConfiguration configuration, ObjectMapper mapper) {
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
