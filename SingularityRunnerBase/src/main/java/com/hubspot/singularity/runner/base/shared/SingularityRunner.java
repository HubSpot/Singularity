package com.hubspot.singularity.runner.base.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class SingularityRunner {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDriver.class);

  public void run(Iterable<? extends Module> modules) {
    final Injector injector = Guice.createInjector(modules);

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
    } catch (Throwable t) {
      LOG.error("Caught unexpected exception, exiting", t);
      System.exit(1);
    }

  }
}
