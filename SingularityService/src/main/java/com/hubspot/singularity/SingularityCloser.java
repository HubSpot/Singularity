package com.hubspot.singularity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityCloser {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityAbort.class);

  private final Injector injector;
  private final long waitSeconds;
  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityCloser(Injector injector, SingularityConfiguration configuration, SingularityExceptionNotifier exceptionNotifier) {
    this.injector = injector;
    this.waitSeconds = configuration.getCloseWaitSeconds();
    this.exceptionNotifier = exceptionNotifier;
  }

  public void closeAllCloseables() {
    ExecutorService es = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("SingularityCloser-%d").build());

    submitCloseables(es);

    shutdown(getClass().getName(), es, waitSeconds + 1);
  }

  public void shutdown(String name, ExecutorService executorService) {
    shutdown(name, executorService, waitSeconds);
  }

  public void shutdown(String name, ExecutorService executorService, long waitSeconds) {
    LOG.info(String.format("Shutting down %s - waiting %s seconds", name, waitSeconds));

    try {
      executorService.shutdown();
      executorService.awaitTermination(waitSeconds, TimeUnit.SECONDS);
      executorService.shutdownNow();
    } catch (Throwable t) {
      LOG.warn(String.format("While shutting down %s executor service", name), t);
      exceptionNotifier.notify(t);
    }
  }

  public void submitCloseables(final ExecutorService es) {
    final List<SingularityCloseable> toClose = getCloseableSingletons();

    LOG.info(String.format("Closing %s closeables", toClose.size()));

    for (final SingularityCloseable close : toClose) {
      es.submit(new Runnable() {

        @Override
        public void run() {
          try {
            close.close();
          } catch (Throwable t) {
            String msg = String.format("Error closing %s", close.getClass().getName());
            LOG.error(msg, t);
            exceptionNotifier.notify(t);
          }
        }
      });
    }
  }

  private List<SingularityCloseable> getCloseableSingletons() {
    final List<SingularityCloseable> toClose = Lists.newArrayList();

    for (Map.Entry<Key<?>, Binding<?>> bindingEntry : injector.getAllBindings().entrySet()) {
      final Key<?> key = bindingEntry.getKey();
      if (SingularityCloseable.class.isAssignableFrom(key.getTypeLiteral().getRawType())) {
        @SuppressWarnings("unchecked")
        final Binding<SingularityCloseable> binding = (Binding<SingularityCloseable>) bindingEntry.getValue();

        if (Scopes.isSingleton(binding)) {
          SingularityCloseable closeable = binding.getProvider().get();
          toClose.add(closeable);
        }
      }
    }

    return toClose;
  }

}
