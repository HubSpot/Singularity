package com.hubspot.singularity.bundles;

import com.google.common.net.HttpHeaders;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;

public class AcceptLanguageFilterBundle implements ConfiguredBundle<SingularityConfiguration> {
  public static final String ES_419 = "es-419";
  public static final String ES_ES = "es-ES";

  @Override
  public void run(SingularityConfiguration configuration, Environment environment) throws Exception {
    environment.jersey().getResourceConfig().getContainerRequestFilters().add(new AcceptLanguageFilter());
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {

  }

  public static class AcceptLanguageFilter implements ContainerRequestFilter {
    @Override
    public ContainerRequest filter(ContainerRequest request) {
      MultivaluedMap<String, String> headers = request.getRequestHeaders();
      if(headers.containsKey(HttpHeaders.ACCEPT_LANGUAGE)) {
        List<String> acceptLanguageValues = headers.remove(HttpHeaders.ACCEPT_LANGUAGE);

        for (int i = 0; i < acceptLanguageValues.size(); i++) {
          final String acceptLanguageValue = acceptLanguageValues.get(i);

          // replace es-419 (invalid) with es_ES (valid, hopefully good enough.)
          if (acceptLanguageValue.contains(ES_419)) {
            acceptLanguageValues.set(i, acceptLanguageValue.replace(ES_419, ES_ES));
          }
        }

        headers.put(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguageValues);

        request.setHeaders((InBoundHeaders)headers);
      }

      return request;
    }
  }
}
