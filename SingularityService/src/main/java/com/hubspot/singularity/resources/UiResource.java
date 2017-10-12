package com.hubspot.singularity.resources;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.views.IndexView;

/**
 * Serves as the base for the UI, returns the mustache view for the actual GUI.
 */
@Singleton
@Path(UiResource.UI_RESOURCE_LOCATION + "{uiPath:.*}")
public class UiResource {

  public static final String UI_RESOURCE_LOCATION = "/ui/";

  private final IndexView indexView;

  @Inject
  public UiResource(IndexView indexView) {
    this.indexView = indexView;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public IndexView getIndex() {
    return indexView;
  }
}
