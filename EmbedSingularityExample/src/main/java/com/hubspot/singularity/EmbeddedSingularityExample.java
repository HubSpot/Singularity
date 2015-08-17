package com.hubspot.singularity;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.hubspot.singularity.config.SingularityConfiguration;

import io.dropwizard.Bundle;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Example on how to embed Singularity into another piece of code or how to extend Singularity with additional
 * functionality that is not in the service out-of-the-box.
 *
 * The {@link SingularityService} class provides a number of extension points to hook additional functionality
 * into the service and by building a separate artifact, it is possible to package the code differently or add
 * additional runtime dependencies without having to modify the main SingularityService build itself.
 */
public class EmbeddedSingularityExample extends SingularityService<SingularityConfiguration> {
  @Override
  public Iterable<? extends Module> getGuiceModules(final Bootstrap<SingularityConfiguration> bootstrap) {
    return ImmutableSet.of();
  }

  @Override
  public Iterable<? extends Bundle> getDropwizardBundles(final Bootstrap<SingularityConfiguration> bootstrap) {
    return ImmutableSet.of();
  }

  @Override
  public Iterable<? extends ConfiguredBundle<SingularityConfiguration>> getDropwizardConfiguredBundles(final Bootstrap<SingularityConfiguration> bootstrap) {
    return ImmutableSet.of();
  }

  @Override
  public void initialize(final Bootstrap<SingularityConfiguration> bootstrap) {
    super.initialize(bootstrap);
  }

  @Override
  public void run(final SingularityConfiguration configuration, final Environment environment) throws Exception {
    super.run(configuration, environment);
  }

  public static void main(final String[] args) throws Exception {
    new EmbeddedSingularityExample().run(args);
  }
}
