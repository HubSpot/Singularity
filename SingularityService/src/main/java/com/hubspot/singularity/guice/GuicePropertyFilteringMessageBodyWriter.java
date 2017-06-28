package com.hubspot.singularity.guice;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.hubspot.jackson.jaxrs.PropertyFilter;
import com.hubspot.jackson.jaxrs.PropertyFiltering;

import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.setup.Environment;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class GuicePropertyFilteringMessageBodyWriter extends JacksonMessageBodyProvider {

  private static final Logger LOG = LoggerFactory.getLogger(GuicePropertyFilteringMessageBodyWriter.class);

  @Context
  private volatile UriInfo uriInfo;

  private final Environment environment;
  private final ObjectMapper objectMapper;

  @Inject
  public GuicePropertyFilteringMessageBodyWriter(final Environment environment, final ObjectMapper objectMapper) {
    super(objectMapper);
    this.environment = checkNotNull(environment, "environment is null");
    this.objectMapper = checkNotNull(objectMapper, "objectMapper is null");
  }

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return hasMatchingMediaType(mediaType) &&
        filteringEnabled(annotations) &&
        super.isWriteable(type, genericType, annotations, mediaType);
  }

  @Override
  public long getSize(final Object object, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(final Object o, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType,
      final MultivaluedMap<String, Object> httpHeaders, final OutputStream os) throws IOException {

    final PropertyFiltering annotation = findPropertyFiltering(annotations);

    final PropertyFilter propertyFilter = new PropertyFilter(Optional.fromNullable(uriInfo.getQueryParameters().get(annotation.using())).or(Collections.<String>emptyList()));

    if (!propertyFilter.hasFilters()) {
      super.writeTo(o, type, genericType, annotations, mediaType, httpHeaders, os);
      return;
    }

    final Timer timer = getTimer();
    final Timer.Context context = timer.time();

    try {
      final JsonNode tree = objectMapper.valueToTree(o);
      propertyFilter.filter(tree);
      super.writeTo(tree, tree.getClass(), tree.getClass(), annotations, mediaType, httpHeaders, os);
    } finally {
      context.stop();
    }
  }

  private Timer getTimer() {
    return getMetricRegistry().timer(MetricRegistry.name(GuicePropertyFilteringMessageBodyWriter.class, "filter"));
  }

  private MetricRegistry getMetricRegistry() {
    MetricRegistry registry = environment.metrics();

    if (registry == null) {
      LOG.warn("No environment metrics found!");
      registry = SharedMetricRegistries.getOrCreate("com.hubspot");
    }

    return registry;
  }

  private static boolean filteringEnabled(final Annotation... annotations) {
    return findPropertyFiltering(annotations) != null;
  }

  private static PropertyFiltering findPropertyFiltering(final Annotation... annotations) {
    for (final Annotation annotation : annotations) {
      if (annotation.annotationType() == PropertyFiltering.class) {
        return (PropertyFiltering) annotation;
      }
    }

    return null;
  }
}
