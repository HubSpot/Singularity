package com.hubspot.singularity;

import com.codahale.metrics.servlets.MetricsServlet;
import io.dropwizard.Bundle;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.server.Server;

import javax.servlet.ServletContext;

/**
 * Expose Dropwizard's metrics registry to {@link com.hubspot.jackson.jaxrs.PropertyFilteringMessageBodyWriter}
 * via the servlet context.
 *
 * This class is picked up by autoconfig
 */
public class MetricsBundle implements Bundle {

  @Override
  public void initialize(Bootstrap<?> bootstrap) { }

  @Override
  public void run(final Environment environment) {
    // need to wait until the server is started to do this otherwise an IllegalStateException is thrown
    environment.lifecycle().addServerLifecycleListener(new ServerLifecycleListener() {
      @Override
      public void serverStarted(Server server) {
        ServletContext context = environment.getJerseyServletContainer().getServletContext();
        context.setAttribute(MetricsServlet.METRICS_REGISTRY, environment.metrics());
      }
    });
  }
}
