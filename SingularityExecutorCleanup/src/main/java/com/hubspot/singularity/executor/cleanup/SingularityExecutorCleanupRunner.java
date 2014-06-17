package com.hubspot.singularity.executor.cleanup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.cleanup.config.SingularityExecutorCleanupConfigurationLoader;
import com.hubspot.singularity.executor.config.SingularityExecutorConfigurationLoader;
import com.hubspot.singularity.executor.config.SingularityExecutorModule;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;


public class SingularityExecutorCleanupRunner {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityExecutorCleanupRunner.class);

  public static void main(String... args) {
    final long start = System.currentTimeMillis();
    
    try {
      final Injector injector = Guice.createInjector(new SingularityRunnerBaseModule(new SingularityExecutorConfigurationLoader(), new SingularityExecutorCleanupConfigurationLoader()), new SingularityExecutorModule());
      final SingularityExecutorCleanup cleanup = injector.getInstance(SingularityExecutorCleanup.class);
      
      LOG.info("Starting cleanup");
      
      cleanup.clean();

      LOG.info("Finished successfully after {}", JavaUtils.duration(start));
      System.exit(0);
    } catch (Throwable t) {
      LOG.error("Finished after {} with error", JavaUtils.duration(start), t);
      System.exit(1);
    }
  }
  
}
