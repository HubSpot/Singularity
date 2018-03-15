package com.hubspot.singularity.resources;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.metrics.SingularityMetricsContainer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.METRICS_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Retrieve metrics from the Singularity scheduler")
@Tags({@Tag(name = "Metrics")})
public class MetricsResource {
  private final MetricRegistry registry;

  @Inject
  public MetricsResource(MetricRegistry registry) {
    this.registry = registry;
  }

  @GET
  @Operation(summary = "Retrieve metrics from this scheduler instance")
  public SingularityMetricsContainer getRegistry() {
    Map<String, Metric> metrics = new HashMap<>(registry.getMetrics());
    // Not an easy way to serialize this particular one since it is a lambda, exclude it for now from the endpoint
    metrics.entrySet().removeIf((e) -> e.getKey().contains("ManagedPooledDataSource"));
    return new SingularityMetricsContainer(metrics);
  }
}
