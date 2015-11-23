package com.hubspot.singularity.jersey;

import static com.hubspot.singularity.jersey.JerseyBinder.bindContainerRequestFilter;
import static com.hubspot.singularity.jersey.JerseyBinder.bindContainerResponseFilter;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;

/**
 * Jersey specific code for Singularity.
 */
public class SingularityJerseyModule implements Module
{
  @Override
  public void configure(final Binder binder)
  {
    bindContainerRequestFilter(binder).to(ReplaceES419LanguageFilter.class).in(Scopes.SINGLETON);
    bindContainerRequestFilter(binder).to(RequestStashFilter.class).in(Scopes.SINGLETON);
    bindContainerResponseFilter(binder).to(RequestStashFilter.class).in(Scopes.SINGLETON);
  }
}
