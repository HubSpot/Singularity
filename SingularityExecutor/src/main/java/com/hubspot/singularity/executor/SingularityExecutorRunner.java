package com.hubspot.singularity.executor;

import org.apache.mesos.MesosExecutorDriver;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.config.SingularityExecutorModule;

public class SingularityExecutorRunner {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityExecutorRunner.class);

  public static void main(String... args) {
    final long start = System.currentTimeMillis();
    
    // TODO register shutdown hook?

    try {
      final Protos.Status driverStatus = new SingularityExecutorRunner().run();

      LOG.info("MesosExecutorDriver finished after {} with status: {}", JavaUtils.duration(start), driverStatus);
      
      System.exit(driverStatus == Protos.Status.DRIVER_STOPPED ? 0 : 1);
    } catch (Throwable t) {
      LOG.error("MesosExecutorDriver finished after {} with error", JavaUtils.duration(start), t);
    }
  }
  
  private SingularityExecutorRunner() {}
  
  public Protos.Status run() {
    final Injector injector = Guice.createInjector(new SingularityExecutorModule());

    LOG.info("Starting MesosExecutorDriver...");
    
    final MesosExecutorDriver driver = new MesosExecutorDriver(injector.getInstance(SingularityExecutor.class));

    return driver.run();
  }
}
