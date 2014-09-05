package com.hubspot.singularity.s3downloader;

import java.util.concurrent.ThreadPoolExecutor;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.s3downloader.config.SingularityS3DownloaderModule;

@Singleton
public class SingularityS3DownloaderMetrics {

  private final MetricRegistry registry;

  private final Timer downloadTimer;

  private final Meter clientErrors;
  private final Meter serverErrors;
  private final Meter requests;

  @Inject
  public SingularityS3DownloaderMetrics(MetricRegistry registry, @Named(SingularityS3DownloaderModule.DOWNLOAD_EXECUTOR_SERVICE) final ThreadPoolExecutor asyncDownloadService) {
    this.registry = registry;

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

}
