package com.hubspot.singularity.jersey;

import javax.ws.rs.container.ContainerRequestFilter;

import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;

/**
 * Convenience binder for the various Jersey SPI specific filters.
 */
public final class JerseyBinder
{
  private JerseyBinder()
  {
    throw new AssertionError("do not instantiate");
  }

  public static LinkedBindingBuilder<ContainerRequestFilter> bindContainerRequestFilter(Binder binder)
  {
    Multibinder<ContainerRequestFilter> requestFilterBinder = Multibinder.newSetBinder(binder, ContainerRequestFilter.class);
    return requestFilterBinder.addBinding();
  }
}
