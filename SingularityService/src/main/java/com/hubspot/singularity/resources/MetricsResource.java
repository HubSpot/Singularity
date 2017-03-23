package com.hubspot.singularity.resources;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.metrics.SingularityMetricsContainer;

@Path(MetricsResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class MetricsResource {
  private static final Logger LOG = LoggerFactory.getLogger(MetricsResource.class);
  public static final String PATH = SingularityService.API_BASE_PATH + "/metrics";

  private final MetricRegistry registry;

  @Inject
  public MetricsResource(MetricRegistry registry) {
    this.registry = registry;
  }

  @GET
  public SingularityMetricsContainer getRegistry() {
    Map<String, Metric> metrics = registry.getMetrics();
    LOG.debug("Found metrics: {}", metrics);
    metrics.entrySet().removeIf((e) -> e.getKey().contains("Lambda"));
    LOG.debug("Found metrics: {}", metrics);
    metrics.entrySet().removeIf((e) -> e.getKey().contains("ManagedPooledDataSource"));
    LOG.debug("Found metrics: {}", metrics);
    return new SingularityMetricsContainer(registry.getMetrics());
  }
}
