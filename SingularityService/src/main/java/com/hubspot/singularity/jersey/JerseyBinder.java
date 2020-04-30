package com.hubspot.singularity.jersey;

import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import javax.ws.rs.container.ContainerRequestFilter;

/**
 * Convenience binder for the various Jersey SPI specific filters.
 */
public final class JerseyBinder {

  private JerseyBinder() {
    throw new AssertionError("do not instantiate");
  }

  public static LinkedBindingBuilder<ContainerRequestFilter> bindContainerRequestFilter(
    Binder binder
  ) {
    Multibinder<ContainerRequestFilter> requestFilterBinder = Multibinder.newSetBinder(
      binder,
      ContainerRequestFilter.class
    );
    return requestFilterBinder.addBinding();
  }
}
