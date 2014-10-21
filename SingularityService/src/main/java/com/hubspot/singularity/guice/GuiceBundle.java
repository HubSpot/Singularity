package com.hubspot.singularity.guice;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
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
  private final ImmutableSet<ConfigurationAwareModule<T>> configurationAwareModules;
  private final ImmutableSet<Module> guiceModules;
  private final Stage guiceStage;

  @Inject
  @Named(GUICE_BUNDLE_NAME)
  private volatile Function<ResourceConfig, ServletContainer> replacer = null;

  private GuiceBundle(final Class<T> configClass, final ImmutableSet<Module> guiceModules, final ImmutableSet<ConfigurationAwareModule<T>> configurationAwareModules, final Stage guiceStage) {
    this.configClass = configClass;

    this.guiceModules = guiceModules;
    this.configurationAwareModules = configurationAwareModules;
    this.guiceStage = guiceStage;
  }

  @Override
  public void initialize(final Bootstrap<?> bootstrap) {}

  @Override
  public void run(final T configuration, final Environment environment) throws Exception {

    for (ConfigurationAwareModule<T> configurationAwareModule : configurationAwareModules) {
      configurationAwareModule.setConfiguration(configuration);
    }

    final DropwizardModule dropwizardModule = new DropwizardModule();

    final Injector injector =
        Guice.createInjector(guiceStage,
            ImmutableSet.<Module>builder()
            .addAll(guiceModules)
            .addAll(configurationAwareModules)
            .add(new GuiceEnforcerModule())
            .add(new JerseyServletModule())
            .add(dropwizardModule).add(new Module() {
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

    for (Managed managed : dropwizardModule.getManaged()) {
      LOG.info("Added guice injected managed Object: {}", managed.getClass().getName());
      environment.lifecycle().manage(managed);
    }

    for (Task task : dropwizardModule.getTasks()) {
      environment.admin().addTask(task);
      LOG.info("Added guice injected Task: {}", task.getClass().getName());
    }

    for (HealthCheck healthcheck : dropwizardModule.getHealthChecks()) {
      environment.healthChecks().register(healthcheck.getClass().getSimpleName(), healthcheck);
      LOG.info("Added guice injected health check: {}", healthcheck.getClass().getName());
    }

    for (ServerLifecycleListener serverLifecycleListener : dropwizardModule.getServerLifecycleListeners()) {
      environment.lifecycle().addServerLifecycleListener(serverLifecycleListener);
    }

    environment.jersey().replace(replacer);
    environment.servlets().addFilter("Guice Filter", GuiceFilter.class).addMappingForUrlPatterns(null, false, environment.getApplicationContext().getContextPath() + "*");
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

  public static class GuiceEnforcerModule implements Module {
    @Override
    public void configure(final Binder binder) {
      binder.disableCircularProxies();
      binder.requireExplicitBindings();
      binder.requireExactBindingAnnotations();
      binder.requireAtInjectOnConstructors();
    }
  }

  public static class DropwizardModule implements Module {
    private final ImmutableSet.Builder<Managed> managedBuilder = ImmutableSet.builder();
    private final ImmutableSet.Builder<Task> taskBuilder = ImmutableSet.builder();
    private final ImmutableSet.Builder<HealthCheck> healthcheckBuilder = ImmutableSet.builder();
    private final ImmutableSet.Builder<ServerLifecycleListener> serverLifecycleListenerBuilder = ImmutableSet.builder();

    @Override
    public void configure(final Binder binder) {
      binder.bindListener(Matchers.any(), new TypeListener() {
        @Override
        public <T> void hear(TypeLiteral<T> type, TypeEncounter<T> encounter) {
          encounter.register(new InjectionListener<T>() {
            @Override
            public void afterInjection(T obj) {
              if (obj instanceof Managed) {
                managedBuilder.add((Managed) obj);
              }

              if (obj instanceof Task) {
                taskBuilder.add((Task) obj);
              }

              if (obj instanceof HealthCheck) {
                healthcheckBuilder.add((HealthCheck) obj);
              }

              if (obj instanceof ServerLifecycleListener) {
                serverLifecycleListenerBuilder.add((ServerLifecycleListener) obj);
              }
            }
          });
        }
      });
    }

    public Set<Managed> getManaged() {
      return managedBuilder.build();
    }

    public Set<Task> getTasks() {
      return taskBuilder.build();
    }

    public Set<HealthCheck> getHealthChecks() {
      return healthcheckBuilder.build();
    }

    public Set<ServerLifecycleListener> getServerLifecycleListeners() {
      return serverLifecycleListenerBuilder.build();
    }
  }

  public static class Builder<U extends Configuration> {
    private final Class<U> configClass;
    private final ImmutableSet.Builder<Module> guiceModules = ImmutableSet.builder();
    private final ImmutableSet.Builder<ConfigurationAwareModule<U>> configurationAwareModules = ImmutableSet.builder();
    private Stage guiceStage = Stage.PRODUCTION;

    private Builder(final Class<U> configClass) {
      this.configClass = configClass;
    }

    public final Builder<U> stage(final Stage guiceStage) {
      checkNotNull(guiceStage, "guiceStage is null");
      if (guiceStage != Stage.PRODUCTION) {
        LOG.warn("Guice should only ever run in PRODUCTION mode except for testing!");
      }
      this.guiceStage = guiceStage;
      return this;
    }

    public final Builder<U> modules(final Module... modules) {
      guiceModules.add(modules);
      return this;
    }

    @SafeVarargs
    public final Builder<U> modules(final ConfigurationAwareModule<U>... modules) {
      configurationAwareModules.add(modules);
      return this;
    }

    public final GuiceBundle<U> build() {
      return new GuiceBundle<U>(configClass, guiceModules.build(), configurationAwareModules.build(), guiceStage);
    }
  }
}
