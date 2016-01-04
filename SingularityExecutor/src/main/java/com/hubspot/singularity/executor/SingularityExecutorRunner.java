package com.hubspot.singularity.executor;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.MesosExecutorDriver;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.config.SingularityExecutorModule;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;

public class SingularityExecutorRunner {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityExecutorRunner.class);

  public static void main(String... args) {
    final long start = System.currentTimeMillis();

    try {
      final Injector injector = Guice.createInjector(Stage.PRODUCTION, new SingularityRunnerBaseModule(SingularityExecutorConfiguration.class, ImmutableSet.<Class<? extends BaseRunnerConfiguration>>of(SingularityS3Configuration.class)), new SingularityExecutorModule());
      final SingularityExecutorRunner executorRunner = injector.getInstance(SingularityExecutorRunner.class);

      final Protos.Status driverStatus = executorRunner.run();

      LOG.info("Executor finished after {} with status: {}", JavaUtils.duration(start), driverStatus);

      System.exit(driverStatus == Protos.Status.DRIVER_STOPPED ? 0 : 1);
    } catch (Throwable t) {
      LOG.error("Finished after {} with error", JavaUtils.duration(start), t);
      System.exit(1);
    }
  }

  private final String name;
  private final SingularityExecutor singularityExecutor;
  private final SingularityExecutorMonitor monitor;

  @Inject
  public SingularityExecutorRunner(@Named(SingularityRunnerBaseModule.PROCESS_NAME) String name, SingularityExecutor singularityExecutor, SingularityExecutorMonitor monitor) {
    this.name = name;
    this.singularityExecutor = singularityExecutor;
    this.monitor = monitor;
  }

  public Protos.Status run() {
    LOG.info("{} starting MesosExecutorDriver...", name);

    final MesosExecutorDriver driver = new MesosExecutorDriver(singularityExecutor);

    Runtime.getRuntime().addShutdownHook(new Thread("SingularityExecutorRunnerGracefulShutdown") {

      @Override
      public void run() {
        LOG.info("Executor is shutting down, ensuring shutdown via shutdown hook");
        monitor.shutdown(Optional.of((ExecutorDriver) driver));
      }

    });

    return driver.run();
  }

}
