package com.hubspot.singularity.jersey;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Replaces language header containing es-419 (LATAM spanish) with es-ES.
 */
public class ReplaceES419LanguageFilter implements ContainerRequestFilter {

  public static final String ES_419 = "es-419";
  public static final String ES_ES = "es-ES";

  @Inject
  ReplaceES419LanguageFilter()
  {}

  @Override
  public void filter(ContainerRequestContext request) {
    MultivaluedMap<String, String> headers = request.getHeaders();
    if (headers.containsKey(HttpHeaders.ACCEPT_LANGUAGE)) {
      List<String> acceptLanguageValues = headers.remove(HttpHeaders.ACCEPT_LANGUAGE);

      for (int i = 0; i < acceptLanguageValues.size(); i++) {
        final String acceptLanguageValue = acceptLanguageValues.get(i);

        // replace es-419 (invalid) with es_ES (valid, hopefully good enough.)
        if (acceptLanguageValue.contains(ES_419)) {
          acceptLanguageValues.set(i, acceptLanguageValue.replace(ES_419, ES_ES));
        }
      }

      headers.put(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguageValues);
    }
  }
}
