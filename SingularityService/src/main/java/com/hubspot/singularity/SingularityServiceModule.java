package com.hubspot.singularity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Function;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.hubspot.mesos.client.SingularityMesosClientModule;
import com.hubspot.mesos.client.UserAndPassword;
import com.hubspot.singularity.auth.dw.SingularityAuthenticatorClass;
import com.hubspot.singularity.config.IndexViewConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.SingularityDataModule;
import com.hubspot.singularity.data.history.SingularityDbModule;
import com.hubspot.singularity.data.history.SingularityHistoryModule;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderModule;
import com.hubspot.singularity.data.zkmigrations.SingularityZkMigrationsModule;
import com.hubspot.singularity.event.SingularityEventModule;
import com.hubspot.singularity.hooks.BaragonLoadBalancerClientImpl;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.hooks.NoOpLoadBalancerClient;
import com.hubspot.singularity.jersey.SingularityJerseyModule;
import com.hubspot.singularity.mesos.SingularityMesosModule;
import com.hubspot.singularity.resources.SingularityOpenApiResource;
import com.hubspot.singularity.resources.SingularityResourceModule;
import com.hubspot.singularity.scheduler.SingularitySchedulerModule;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class SingularityServiceModule
  extends DropwizardAwareModule<SingularityConfiguration> {
  public static final String REQUESTS_CAFFEINE_CACHE = "requests_caffeine_cache";
  private final Function<SingularityConfiguration, Module> dbModuleProvider;
  private Optional<Class<? extends LoadBalancerClient>> lbClientClass = Optional.empty();

  public SingularityServiceModule() {
    this.dbModuleProvider = SingularityDbModule::new;
  }

  public SingularityServiceModule(
    Function<SingularityConfiguration, Module> dbModuleProvider
  ) {
    this.dbModuleProvider = dbModuleProvider;
  }

  public void setLoadBalancerClientClass(
    Class<? extends LoadBalancerClient> lbClientClass
  ) {
    this.lbClientClass = Optional.of(lbClientClass);
  }

  @Override
  public void configure(Binder binder) {
    SingularityConfiguration configuration = getConfiguration();
    binder.install(
      new SingularityMainModule(
        getConfiguration(),
        lbClientClass.orElseGet(
          () ->
            configuration.getLoadBalancerUri() != null
              ? BaragonLoadBalancerClientImpl.class
              : NoOpLoadBalancerClient.class
        )
      )
    );
    binder.install(new SingularityDataModule(getConfiguration()));
    binder.install(new SingularitySchedulerModule());
    binder.install(
      new SingularityResourceModule(getConfiguration().getUiConfiguration())
    );
    binder.install(new SingularityTranscoderModule());
    binder.install(new SingularityHistoryModule());
    binder.install(dbModuleProvider.apply(getConfiguration()));
    binder.install(new SingularityMesosModule());
    binder.install(new SingularityZkMigrationsModule());
    binder.install(new SingularityJerseyModule());

    MesosConfiguration mesosConfiguration = getConfiguration().getMesosConfiguration();
    if (
      mesosConfiguration.getMesosUsername().isPresent() &&
      mesosConfiguration.getMesosPassword().isPresent()
    ) {
      binder.install(
        new SingularityMesosClientModule(
          new UserAndPassword(
            mesosConfiguration.getMesosUsername().get(),
            mesosConfiguration.getMesosPassword().get()
          )
        )
      );
    } else {
      binder.install(new SingularityMesosClientModule());
    }

    // API Docs
    getEnvironment().jersey().register(SingularityOpenApiResource.class);

    binder.install(
      new SingularityEventModule(getConfiguration().getWebhookQueueConfiguration())
    );
  }

  @Provides
  @Singleton
  public IndexViewConfiguration provideIndexViewConfiguration(
    LoadBalancerClient loadBalancerClient
  ) {
    SingularityConfiguration configuration = getConfiguration();
    return new IndexViewConfiguration(
      configuration.getUiConfiguration(),
      configuration.getMesosConfiguration().getDefaultMemory(),
      configuration.getMesosConfiguration().getDefaultCpus(),
      configuration.getMesosConfiguration().getDefaultDisk(),
      configuration.getMesosConfiguration().getAgentHttpPort(),
      configuration.getMesosConfiguration().getAgentHttpsPort(),
      configuration.getDefaultBounceExpirationMinutes(),
      configuration.getHealthcheckIntervalSeconds(),
      configuration.getHealthcheckTimeoutSeconds(),
      configuration.getHealthcheckMaxRetries(),
      configuration.getStartupTimeoutSeconds(),
      loadBalancerClient.isEnabled(),
      configuration.getCommonHostnameSuffixToOmit(),
      configuration.getWarnIfScheduledJobIsRunningPastNextRunPct(),
      configuration.getAuthConfiguration().isEnabled() &&
      configuration
        .getAuthConfiguration()
        .getAuthenticators()
        .contains(SingularityAuthenticatorClass.WEBHOOK)
    );
  }

  @Provides
  @Singleton
  @Named(REQUESTS_CAFFEINE_CACHE)
  public Cache<String, List<SingularityRequestParent>> getRequestsCaffeineCache() {
    SingularityConfiguration configuration = getConfiguration();

    return Caffeine
      .newBuilder()
      .expireAfterWrite(configuration.getCaffeineCacheTtl(), TimeUnit.SECONDS)
      .build();
  }
}
