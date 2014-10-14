package com.hubspot.singularity;

<<<<<<< HEAD
import com.google.inject.Binder;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.SingularityDataModule;
import com.hubspot.singularity.data.history.SingularityHistoryModule;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderModule;
import com.hubspot.singularity.guice.ConfigurationAwareModule;
import com.hubspot.singularity.mesos.SingularityMesosModule;
import com.hubspot.singularity.resources.SingularityResourceModule;
import com.hubspot.singularity.scheduler.SingularitySchedulerModule;

public class SingularityServiceModule extends ConfigurationAwareModule<SingularityConfiguration> {

  @Override
  protected void configure(Binder binder, SingularityConfiguration configuration) {
    binder.install(new SingularityMainModule());
    binder.install(new SingularityDataModule());
    binder.install(new SingularitySchedulerModule());
    binder.install(new SingularityResourceModule());
    binder.install(new SingularityTranscoderModule());
    binder.install(new SingularityHistoryModule(configuration));
    binder.install(new SingularityMesosModule());
  }
}
