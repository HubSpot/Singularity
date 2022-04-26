package com.hubspot.singularity.managed;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.thread.ShutdownThread;

/**
 * This class runs shutdown hooks before the default Jetty server shutdown hook / lifecycle management.
 * Actions will be run serially - any exceptions will be swallowed until after all have run.
 *
 * This class does not need to be created after the Jetty server.
 */
@Singleton
public class SingularityPreJettyLifecycle extends AbstractLifeCycle implements Managed {

  private final List<Runnable> hooks;
  private final AtomicBoolean hasRun;

  @Inject
  public SingularityPreJettyLifecycle() {
    this.hooks = new CopyOnWriteArrayList<>();
    this.hasRun = new AtomicBoolean();
  }

  @Override
  protected void doStart() throws Exception {
    // Registering at index 0 ensures this hook is run first during shutdown.
    ShutdownThread.register(0, this);
  }

  @Override
  protected synchronized void doStop() throws Exception {
    if (hasRun.get()) {
      return;
    }

    MultiException exceptions = new MultiException();
    for (Runnable hook : hooks) {
      try {
        hook.run();
      } catch (Exception e) {
        exceptions.add(e);
      }
    }
    hasRun.set(true);
    exceptions.ifExceptionThrow();
  }

  public boolean registerShutdownHook(Runnable runnable) {
    return hooks.add(runnable);
  }
}
