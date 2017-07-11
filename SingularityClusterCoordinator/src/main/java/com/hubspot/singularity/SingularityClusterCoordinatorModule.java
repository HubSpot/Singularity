package com.hubspot.singularity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.hubspot.singularity.proxy.SingularityClusterCoodinatorResourcesModule;

public class SingularityClusterCoordinatorModule extends DropwizardAwareModule<ClusterCoordinatorConfiguration> {

  @Override
  public void configure(Binder binder) {
    binder.bind(ObjectMapper.class).toInstance(JavaUtils.newObjectMapper());
    binder.install(new SingularityClusterCoodinatorResourcesModule(getConfiguration()));
  }
}
