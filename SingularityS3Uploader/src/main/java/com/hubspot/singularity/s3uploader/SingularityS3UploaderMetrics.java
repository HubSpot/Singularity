package com.hubspot.singularity.s3uploader;

import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;

public class SingularityS3UploaderMetrics {

  private final MetricRegistry registry;
  private final Counter uploaderCounter;
  private final Counter expiringUploaderCounter;
  private final Timer uploadTimer;
  private final Meter filesystemEventsMeter;
  
  @Inject
  public SingularityS3UploaderMetrics(MetricRegistry registry) {
    this.registry = registry;
    this.uploaderCounter = registry.counter(name("uploaders", "total"));
    this.expiringUploaderCounter = registry.counter(name("uploaders", "expiring"));
    this.uploadTimer = registry.timer(name("uploads"));
    this.filesystemEventsMeter = registry.meter(name("filesystem", "events"));
    
    startJmxReporter();
  }
  
  private String name(String... names) {
    return MetricRegistry.name(SingularityS3UploaderMetrics.class, names);
  }

  private void startJmxReporter() {
    JmxReporter reporter = JmxReporter.forRegistry(registry).build();
    reporter.start();
  }
    
  public Counter getExpiringUploaderCounter() {
    return expiringUploaderCounter;
  }

  public Counter getUploaderCounter() {
    return uploaderCounter;
  }
  
  public Timer getUploadTimer() {
    return uploadTimer;
  }

  public Meter getFilesystemEventsMeter() {
    return filesystemEventsMeter;
  }
  
}
