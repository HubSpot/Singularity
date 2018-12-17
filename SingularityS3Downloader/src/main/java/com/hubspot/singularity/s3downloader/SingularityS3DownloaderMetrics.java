package com.hubspot.singularity.s3downloader;

import static com.hubspot.singularity.s3downloader.config.SingularityS3DownloaderModule.METRICS_OBJECT_MAPPER;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.s3downloader.config.SingularityS3DownloaderConfiguration;
import com.hubspot.singularity.s3downloader.config.SingularityS3DownloaderModule;

@Singleton
public class SingularityS3DownloaderMetrics {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityS3DownloaderMetrics.class);

  private final ScheduledExecutorService fileReporterExecutor;

  private final MetricRegistry registry;
  private final ObjectMapper mapper;
  private final SingularityS3DownloaderConfiguration downloaderConfiguration;

  private final Timer downloadTimer;

  private final Meter clientErrors;
  private final Meter serverErrors;
  private final Meter requests;

  @Inject
  public SingularityS3DownloaderMetrics(MetricRegistry registry,
                                        @Named(METRICS_OBJECT_MAPPER) ObjectMapper mapper,
                                        @Named(SingularityS3DownloaderModule.DOWNLOAD_EXECUTOR_SERVICE) final ThreadPoolExecutor asyncDownloadService,
                                        SingularityS3DownloaderConfiguration downloaderConfiguration) {
    this.fileReporterExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("metrics-file-reporter").build());

    this.registry = registry;
    this.mapper = mapper;
    this.downloaderConfiguration = downloaderConfiguration;

    this.downloadTimer = registry.timer(name("downloads", "timer"));

    this.clientErrors = registry.meter(name("server", "clientErrors"));
    this.serverErrors = registry.meter(name("server", "serverErrors"));
    this.requests = registry.meter(name("server", "requests"));

    registry.register(name("downloads", "active"), new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return asyncDownloadService.getActiveCount();
      }
    });

    startJmxReporter();

    if (downloaderConfiguration.getMetricsFilePath().isPresent()) {
      startFileReporter();
    }
  }

  public Meter getClientErrorsMeter() {
    return clientErrors;
  }

  public Meter getServerErrorsMeter() {
    return serverErrors;
  }

  public Meter getRequestsMeter() {
    return requests;
  }

  public Timer getDownloadTimer() {
    return downloadTimer;
  }

  private String name(String... names) {
    return MetricRegistry.name(SingularityS3DownloaderMetrics.class, names);
  }

  private void startJmxReporter() {
    JmxReporter reporter = JmxReporter.forRegistry(registry).build();
    reporter.start();
  }

  private void startFileReporter() {
    fileReporterExecutor.scheduleAtFixedRate(() -> {

      File metricsFile = new File(downloaderConfiguration.getMetricsFilePath().get());

      try (Writer metricsFileWriter = new FileWriter(metricsFile, false)) {
        metricsFileWriter.write(mapper.writeValueAsString(registry.getMetrics()));
        metricsFileWriter.flush();
      } catch (IOException e) {
        LOG.error("Unable to write metrics to file", e);
      }
    }, 10, 30, TimeUnit.SECONDS);
  }

}
