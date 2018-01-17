package com.hubspot.singularity;

import com.google.common.base.Strings;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.hubspot.mesos.client.SingularityMesosClientModule;
import com.hubspot.singularity.auth.SingularityAuthenticatorClass;
import com.hubspot.singularity.config.IndexViewConfiguration;
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
  }

  @Provides
  @Singleton
  public IndexViewConfiguration provideIndexViewConfiguration() {
    SingularityConfiguration configuration = getConfiguration();
    return new IndexViewConfiguration(
        configuration.getUiConfiguration(),
        configuration.getMesosConfiguration().getDefaultMemory(),
        configuration.getMesosConfiguration().getDefaultCpus(),
        configuration.getMesosConfiguration().getDefaultDisk(),
        configuration.getMesosConfiguration().getSlaveHttpPort(),
        configuration.getMesosConfiguration().getSlaveHttpsPort(),
        configuration.getDefaultBounceExpirationMinutes(),
        configuration.getHealthcheckIntervalSeconds(),
        configuration.getHealthcheckTimeoutSeconds(),
        configuration.getHealthcheckMaxRetries(),
        configuration.getStartupTimeoutSeconds(),
        !Strings.isNullOrEmpty(configuration.getLoadBalancerUri()),
        configuration.getCommonHostnameSuffixToOmit(),
        configuration.getWarnIfScheduledJobIsRunningPastNextRunPct(),
        configuration.getAuthConfiguration().isEnabled() && configuration.getAuthConfiguration().getAuthenticators().contains(SingularityAuthenticatorClass.WEBHOOK)
    );
  }
}
