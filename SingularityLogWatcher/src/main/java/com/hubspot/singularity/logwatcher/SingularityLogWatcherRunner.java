package com.hubspot.singularity.logwatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherModule;
import com.hubspot.singularity.logwatcher.config.test.SingularityLogWatcherTestModule;
import com.hubspot.singularity.logwatcher.driver.SingularityLogWatcherDriver;

public class SingularityLogWatcherRunner {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityLogWatcherDriver.class);
  
  public static void main(String... args) {
    new SingularityLogWatcherRunner().run(args);
  }
  
  private SingularityLogWatcherRunner() {}
  
  public void run(String[] args) {
    final Injector injector = Guice.createInjector(new SingularityLogWatcherModule(), new SingularityLogWatcherTestModule(args));
    
    final SingularityLogWatcherDriver driver = injector.getInstance(SingularityLogWatcherDriver.class);
    
    Runtime.getRuntime().addShutdownHook(new Thread("SingularityLogWatcherGracefulShutdown") {

      @Override
      public void run() {
        driver.shutdown();
      }
      
    });
    
    try {
      driver.start();
    } catch (Throwable t) {
      LOG.error("Caught unexpected exception, exiting", t);
      driver.shutdown();
    }
  }

}
