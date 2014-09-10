package com.hubspot.singularity.guice;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.hubspot.singularity.guice.ReflectionHelpers.scanBindings;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceFilter;
import com.hubspot.singularity.guice.ReflectionHelpers.Callback;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 * Dropwizard bundle that adds basic guice integration.
 */
public class GuiceBundle<T extends Configuration> implements ConfiguredBundle<T> {
  private static final String GUICE_BUNDLE_NAME = "_guice_bundle";
  private static final Named GUICE_BUNDLE_NAMED = Names.named(GUICE_BUNDLE_NAME);

  private static final Logger LOG = LoggerFactory.getLogger(GuiceBundle.class);

  public static final <U extends Configuration> Builder<U> defaultBuilder(final Class<U> configClass) {
    return new Builder<>(configClass);
  }

  private final Class<T> configClass;
  private final ImmutableSet<Module> guiceModules;
  private final Stage guiceStage;

  @Inject
  @Named(GUICE_BUNDLE_NAME)
  private volatile Function<ResourceConfig, ServletContainer> replacer = null;

  private GuiceBundle(final Class<T> configClass, final ImmutableSet<Module> guiceModules, final Stage guiceStage) {
    this.configClass = configClass;
    this.guiceModules = guiceModules;
    this.guiceStage = guiceStage;
  }

  @Override
  public void initialize(final Bootstrap<?> bootstrap) {}

  @Override
  public void run(final T configuration, final Environment environment) throws Exception {

    final Injector injector = Guice.createInjector(guiceStage,
        ImmutableSet.<Module>builder()
            .addAll(guiceModules)
            .add(new GuiceEnforcerModule())
            .add(new JerseyServletModule())
            .add(new Module() {
              @Override
              public void configure(final Binder binder) {
                binder.bind(Environment.class).toInstance(environment);
                binder.bind(configClass).toInstance(configuration);

                binder.bind(GuiceContainer.class).to(DropwizardGuiceContainer.class).in(Scopes.SINGLETON);

                binder.bind(new TypeLiteral<Function<ResourceConfig, ServletContainer>>() {}).annotatedWith(GUICE_BUNDLE_NAMED).to(GuiceContainerReplacer.class).in(Scopes.SINGLETON);
              }
            }).build());

    injector.injectMembers(this);

    checkState(replacer != null, "No guice container replacer was injected!");

    environment.jersey().replace(replacer);
    environment.servlets().addFilter("Guice Filter", GuiceFilter.class).addMappingForUrlPatterns(null, false, environment.getApplicationContext().getContextPath() + "*");

    scanBindings(injector, Task.class, new Callback<Binding<?>>() {
      @Override
      public void call(final Binding<?> binding) {
        final Task injectedTask = (Task) injector.getInstance(binding.getKey());
        environment.admin().addTask(injectedTask);
        LOG.info("Added guice injected Task: {}", binding.getKey());
      }
    });

    scanBindings(injector, Managed.class, new Callback<Binding<?>>() {
      @Override
      public void call(final Binding<?> binding) {
        final Managed injectedManaged = (Managed) injector.getInstance(binding.getKey());
        environment.lifecycle().manage(injectedManaged);
        LOG.info("Added guice injected managed Object: {}", binding.getKey());
      }
    });

    scanBindings(injector, HealthCheck.class, new Callback<Binding<?>>() {
      @Override
      public void call(final Binding<?> binding) {
        final HealthCheck injectedHealthCheck = (HealthCheck) injector.getInstance(binding.getKey());
        environment.healthChecks().register(injectedHealthCheck.getClass().getSimpleName(), injectedHealthCheck);
        LOG.info("Added guice injected health check: {}", binding.getKey());
      }
    });
  }

  private static class GuiceContainerReplacer implements Function<ResourceConfig, ServletContainer> {
    private final GuiceContainer container;

    @Inject
    GuiceContainerReplacer(final GuiceContainer container) {
      this.container = checkNotNull(container, "container is null");
    }

    @Override
    public ServletContainer apply(@Nonnull final ResourceConfig resourceConfig) {
      return container;
    }
  }

  private static class GuiceEnforcerModule implements Module {
    @Override
    public void configure(final Binder binder) {
      binder.disableCircularProxies();
      binder.requireExplicitBindings();
      binder.requireExactBindingAnnotations();
      binder.requireAtInjectOnConstructors();
    }
  }

  public static class Builder<U extends Configuration> {
    private final Class<U> configClass;
    private final ImmutableSet.Builder<Module> guiceModules = ImmutableSet.builder();
    private Stage guiceStage = Stage.PRODUCTION;

    private Builder(final Class<U> configClass) {
      this.configClass = configClass;
    }

    public Builder<U> stage(final Stage guiceStage) {
      checkNotNull(guiceStage, "guiceStage is null");
      if (guiceStage != Stage.PRODUCTION) {
        LOG.warn("Guice should only ever run in PRODUCTION mode except for testing!");
      }
      this.guiceStage = guiceStage;
      return this;
    }

    public Builder<U> modules(final Module... modules) {
      guiceModules.add(modules);
      return this;
    }

    public GuiceBundle<U> build() {
      return new GuiceBundle<U>(configClass, guiceModules.build(), guiceStage);
    }
  }
}
