package com.hubspot.singularity.resources.ui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.resources.SingularityServiceUIModule;
import com.hubspot.singularity.config.IndexViewConfiguration;
import com.hubspot.singularity.views.IndexView;

/**
 * Serves as the base for the UI, returns the mustache view for the actual GUI.
 */
@Singleton
@Path(UiResource.UI_RESOURCE_LOCATION + "{uiPath:.*}")
public class UiResource {

  public static final String UI_RESOURCE_LOCATION = "/ui/";

  private final IndexViewConfiguration configuration;
  private final String singularityUriBase;
  private final ObjectMapper mapper;

  @Inject
  public UiResource(@Named(SingularityServiceUIModule.SINGULARITY_URI_BASE) String singularityUriBase, IndexViewConfiguration configuration, ObjectMapper mapper) {
    this.configuration = configuration;
    this.singularityUriBase = singularityUriBase;
    this.mapper = mapper;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public IndexView getIndex() {
    return new IndexView(singularityUriBase, UI_RESOURCE_LOCATION, configuration, mapper);
  }
}
