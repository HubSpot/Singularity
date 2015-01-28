package com.hubspot.singularity.event;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceProvider;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.WebhookManager;

public class SingularityEventModule implements Module {
  public static final String LISTENER_THREADPOOL_NAME = "_listener_threadpool";
  public static final Named LISTENER_THREADPOOL_NAMED = Names.named(LISTENER_THREADPOOL_NAME);

  private final SingularityConfiguration configuration;

  public SingularityEventModule(final SingularityConfiguration configuration) {
    this.configuration = checkNotNull(configuration, "configuration is null");
  }

  @Override
  public void configure(final Binder binder) {
    Multibinder<SingularityEventListener> eventListeners = Multibinder.newSetBinder(binder, SingularityEventListener.class);
    eventListeners.addBinding().to(WebhookManager.class).in(Scopes.SINGLETON);

    binder.bind(SingularityEventListener.class).to(SingularityEventController.class).in(Scopes.SINGLETON);

    binder.bind(ScheduledExecutorService.class).annotatedWith(LISTENER_THREADPOOL_NAMED).toProvider(new SingularityManagedScheduledExecutorServiceProvider(configuration.getListenerThreadpoolSize(), configuration.getThreadpoolShutdownDelayInSeconds(), "listener")).in(Scopes.SINGLETON);
  }
}
