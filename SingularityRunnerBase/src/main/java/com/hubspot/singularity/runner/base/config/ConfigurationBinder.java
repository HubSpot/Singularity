package com.hubspot.singularity.runner.base.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;

public class ConfigurationBinder {
  private final Binder binder;
  private final Multibinder<BaseRunnerConfiguration> multibinder;

  public static <T extends BaseRunnerConfiguration> ConfigurationBinder newBinder(Binder binder) {
    return new ConfigurationBinder(binder);
  }

  private ConfigurationBinder(Binder binder) {
    this.binder = binder;
    this.multibinder = Multibinder.newSetBinder(binder, BaseRunnerConfiguration.class);
  }

  public <T extends BaseRunnerConfiguration> ConfigurationBinder bindPrimaryConfiguration(Class<T> configurationClass, Optional<String> filename) {
    bindConfiguration(configurationClass, filename);
    binder.bind(BaseRunnerConfiguration.class).to(configurationClass);
    return this;
  }

  public <T extends BaseRunnerConfiguration> ConfigurationBinder bindConfiguration(Class<T> configurationClass, Optional<String> filename) {
    checkNotNull(configurationClass, "configurationClass");
    checkState(configurationClass.getAnnotation(Configuration.class) != null, "%s must be annotated with @Configuration", configurationClass.getSimpleName());

    binder.bind(configurationClass).toProvider(new SingularityRunnerConfigurationProvider<T>(configurationClass, filename)).in(Scopes.SINGLETON);
    multibinder.addBinding().to(configurationClass);
    return this;
  }
}
