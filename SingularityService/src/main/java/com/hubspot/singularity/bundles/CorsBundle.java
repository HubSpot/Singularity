package com.hubspot.singularity.bundles;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import org.eclipse.jetty.servlets.CrossOriginFilter;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;
import com.hubspot.singularity.config.CorsConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Adds a CORS filter.
 */
public class CorsBundle implements ConfiguredBundle<SingularityConfiguration> {

  private static final String FILTER_NAME = "Cross Origin Request Filter";

  @Override
  public void initialize(final Bootstrap<?> bootstrap) {}

  @Override
  public void run(final SingularityConfiguration config, final Environment environment) {
    CorsConfiguration corsConfiguration = config.getCors();
    if (!config.isEnableCorsFilter() && !corsConfiguration.isEnabled()) {
      return;
    }

    final Filter corsFilter = new CrossOriginFilter();
    final FilterConfig corsFilterConfig = new FilterConfig() {

      @Override
      public String getFilterName() {
        return FILTER_NAME;
      }

      @Override
      public ServletContext getServletContext() {
        return null;
      }

      @Override
      public String getInitParameter(final String name) {
        return null;
      }

      @Override
      public Enumeration<String> getInitParameterNames() {
        return Iterators.asEnumeration(Collections.<String>emptyIterator());
      }
    };

    try {
      corsFilter.init(corsFilterConfig);
    } catch (final Exception e) {
      throw Throwables.propagate(e);
    }

    FilterRegistration.Dynamic filter = environment.servlets().addFilter(FILTER_NAME, corsFilter);

    filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, corsConfiguration.getAllowedOrigins());
    filter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, corsConfiguration.getAllowedHeaders());
    filter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, corsConfiguration.getAllowedMethods());
    filter.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, corsConfiguration.isAllowCredentials() ? "true" : "false");

    filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
  }
}
