package com.hubspot.singularity.runner.base.shared;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.hubspot.singularity.runner.base.config.MissingConfigException;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class SingularityRunner {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDriver.class);

  @SuppressFBWarnings("DM_EXIT")
  public void run(Iterable<? extends Module> modules) {
    final Injector injector = Guice.createInjector(Stage.PRODUCTION, modules);

    final SingularityRunnerExceptionNotifier exceptionNotifier = injector.getInstance(SingularityRunnerExceptionNotifier.class);

    final SingularityDriver driver = injector.getInstance(SingularityDriver.class);

    Runtime.getRuntime().addShutdownHook(new Thread("SingularityRunnerGracefulShutdown") {

      @Override
      public void run() {
        driver.shutdown();
      }

    });

    try {
      driver.startAndWait();

      LOG.info("Exiting normally");

      System.exit(0);
    } catch (MissingConfigException mce) {
      LOG.error("Missing required configuration, exiting", mce);
      System.exit(1);
    } catch (Throwable t) {
      LOG.error("Caught unexpected exception, exiting", t);
      exceptionNotifier.notify(String.format("Unexpected exception in runner (%s)", t.getMessage()), t, Collections.<String, String>emptyMap());
      System.exit(1);
    }

  }
}
