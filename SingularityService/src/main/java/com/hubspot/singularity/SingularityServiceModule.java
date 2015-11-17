package com.hubspot.singularity;

import com.google.inject.Binder;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.hubspot.mesos.client.SingularityMesosClientModule;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.SingularityDataModule;
import com.hubspot.singularity.data.history.SingularityHistoryModule;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderModule;
import com.hubspot.singularity.data.zkmigrations.SingularityZkMigrationsModule;
import com.hubspot.singularity.event.SingularityEventModule;
import com.hubspot.singularity.jersey.SingularityJerseyModule;
import com.hubspot.singularity.mesos.SingularityMesosModule;
import com.hubspot.singularity.resources.SingularityResourceModule;
import com.hubspot.singularity.scheduler.SingularitySchedulerModule;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;

public class SingularityServiceModule extends DropwizardAwareModule<SingularityConfiguration> {

  @Override
  public void configure(Binder binder) {
    binder.install(new MetricsInstrumentationModule(getBootstrap().getMetricRegistry()));

    binder.install(new SingularityMainModule(getConfiguration()));
    binder.install(new SingularityDataModule());
    binder.install(new SingularitySchedulerModule());
    binder.install(new SingularityResourceModule(getConfiguration().getUiConfiguration()));
    binder.install(new SingularityTranscoderModule());
    binder.install(new SingularityHistoryModule(getConfiguration()));
    binder.install(new SingularityMesosModule());
    binder.install(new SingularityZkMigrationsModule());
    binder.install(new SingularityMesosClientModule());
    binder.install(new SingularityJerseyModule());

    binder.install(new SingularityEventModule(getConfiguration()));

    binder.install(new SingularityAuthModule(getConfiguration()));
  }
}
