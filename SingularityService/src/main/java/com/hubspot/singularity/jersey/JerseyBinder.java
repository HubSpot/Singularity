package com.hubspot.singularity.jersey;

import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

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

  public static LinkedBindingBuilder<ContainerResponseFilter> bindContainerResponseFilter(Binder binder)
  {
    Multibinder<ContainerResponseFilter> responseFilterBinder = Multibinder.newSetBinder(binder, ContainerResponseFilter.class);
    return responseFilterBinder.addBinding();
  }

  public static LinkedBindingBuilder<ResourceFilterFactory> bindResourceFilter(Binder binder)
  {
    Multibinder<ResourceFilterFactory> resourceFilterBinder = Multibinder.newSetBinder(binder, ResourceFilterFactory.class);
    return resourceFilterBinder.addBinding();
  }
}
