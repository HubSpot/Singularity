package com.hubspot.singularity.executor;

import org.apache.mesos.MesosExecutorDriver;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.config.SingularityExecutorConfigurationLoader;
import com.hubspot.singularity.executor.config.SingularityExecutorModule;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.s3.base.config.SingularityS3ConfigurationLoader;

public class SingularityExecutorRunner {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityExecutorRunner.class);

  public static void main(String... args) {
    final long start = System.currentTimeMillis();

    try {
      final Injector injector = Guice.createInjector(new SingularityRunnerBaseModule(new SingularityS3ConfigurationLoader(), new SingularityExecutorConfigurationLoader()), new SingularityExecutorModule());
      final SingularityExecutorRunner executorRunner = injector.getInstance(SingularityExecutorRunner.class);

      final Protos.Status driverStatus = executorRunner.run();

      LOG.info("Finished after {} with status: {}", JavaUtils.duration(start), driverStatus);

      System.exit(driverStatus == Protos.Status.DRIVER_STOPPED ? 0 : 1);
    } catch (Throwable t) {
      LOG.error("Finished after {} with error", JavaUtils.duration(start), t);
      System.exit(1);
    }
  }

  private final String name;
  private final SingularityExecutor singularityExecutor;

  @Inject
  public SingularityExecutorRunner(@Named(SingularityRunnerBaseModule.PROCESS_NAME) String name, SingularityExecutor singularityExecutor) {
    this.name = name;
    this.singularityExecutor = singularityExecutor;
  }

  public Protos.Status run() {
    LOG.info("{} starting MesosExecutorDriver...", name);

    final MesosExecutorDriver driver = new MesosExecutorDriver(singularityExecutor);

    return driver.run();
  }

}
