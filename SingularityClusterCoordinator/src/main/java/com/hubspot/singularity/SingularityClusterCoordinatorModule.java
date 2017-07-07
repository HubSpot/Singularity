package com.hubspot.singularity;

import com.google.inject.Binder;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.hubspot.singularity.proxy.SingularityClusterCoodinatorResourcesModule;

public class SingularityClusterCoordinatorModule extends DropwizardAwareModule<ClusterCoordinatorConfiguration> {

  @Override
  public void configure(Binder binder) {
    binder.install(new SingularityClusterCoodinatorResourcesModule());
  }
}
