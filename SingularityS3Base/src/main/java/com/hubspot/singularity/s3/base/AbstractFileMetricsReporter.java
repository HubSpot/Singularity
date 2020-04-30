package com.hubspot.singularity.s3.base;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFileMetricsReporter {
  private static final Logger LOG = LoggerFactory.getLogger(
    AbstractFileMetricsReporter.class
  );

  protected final SingularityS3Configuration configuration;
  protected final ObjectMapper metricsObjectMapper;
  protected final MetricRegistry registry;
  protected final ScheduledExecutorService fileReporterExecutor;

  public AbstractFileMetricsReporter(
    MetricRegistry registry,
    SingularityS3Configuration configuration,
    ObjectMapper metricsObjectMapper
  ) {
    this.registry = registry;
    this.configuration = configuration;
    this.metricsObjectMapper = metricsObjectMapper;

    this.fileReporterExecutor =
      Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("metrics-file-reporter").build()
      );

    if (configuration.getMetricsFilePath().isPresent()) {
      startFileReporter();
    }
  }

  private void startFileReporter() {
    fileReporterExecutor.scheduleAtFixedRate(
      () -> {
        File metricsFile = new File(configuration.getMetricsFilePath().get());

        try (Writer metricsFileWriter = new FileWriter(metricsFile, false)) {
          metricsFileWriter.write(
            metricsObjectMapper.writeValueAsString(registry.getMetrics())
          );
          metricsFileWriter.flush();
        } catch (IOException e) {
          LOG.error("Unable to write metrics to file", e);
        }
      },
      10,
      30,
      TimeUnit.SECONDS
    );
  }
}
