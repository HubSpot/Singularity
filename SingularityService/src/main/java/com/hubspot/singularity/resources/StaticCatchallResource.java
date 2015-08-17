package com.hubspot.singularity.resources;

import static com.hubspot.singularity.SingularityMainModule.SINGULARITY_URI_BASE;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.views.IndexView;

@Singleton
@Path("/{uiPath:.*}")
public class StaticCatchallResource {
  private final SingularityConfiguration configuration;
  private final String singularityUriBase;

  @Inject
  public StaticCatchallResource(@Named(SINGULARITY_URI_BASE) String singularityUriBase, SingularityConfiguration configuration) {
    this.configuration = configuration;
    this.singularityUriBase = singularityUriBase;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public IndexView getIndex() {
    return new IndexView(singularityUriBase, "", configuration);
  }
}
