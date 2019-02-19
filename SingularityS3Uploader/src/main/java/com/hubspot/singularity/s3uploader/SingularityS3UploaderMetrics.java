package com.hubspot.singularity.s3uploader;

import static com.hubspot.singularity.s3.base.SingularityS3BaseModule.METRICS_OBJECT_MAPPER;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.s3.base.AbstractFileMetricsReporter;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;

@Singleton
public class SingularityS3UploaderMetrics extends AbstractFileMetricsReporter {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityS3UploaderMetrics.class);

  private final MetricRegistry registry;
  private final Counter uploaderCounter;
  private final Counter uploadCounter;
  private final Counter immediateUploaderCounter;
  private final Counter errorCounter;
  private final Timer uploadTimer;
  private final Meter filesystemEventsMeter;

  private Optional<Collection<SingularityUploader>> expiring;

  private long timeOfLastSuccessUpload;
  private int lastUploadDuration;
  private long startUploadsAt;

  @Inject
  public SingularityS3UploaderMetrics(MetricRegistry registry,
                                      @Named(METRICS_OBJECT_MAPPER) ObjectMapper mapper,
                                      SingularityS3Configuration baseConfiguration) {
    super(registry, baseConfiguration, mapper);

    this.registry = registry;
    this.uploaderCounter = registry.counter(name("uploaders", "total"));
    this.immediateUploaderCounter = registry.counter(name("uploaders", "immediate"));
    this.uploadCounter = registry.counter(name("uploads", "success"));
    this.errorCounter = registry.counter(name("uploads", "errors"));
    this.uploadTimer = registry.timer(name("uploads", "timer"));

    this.expiring = Optional.absent();
    this.timeOfLastSuccessUpload = -1;

    registry.register(name("uploads", "millissincelast"), new Gauge<Integer>() {

      @Override
      public Integer getValue() {
        if (timeOfLastSuccessUpload == -1) {
          return -1;
        }

        return Integer.valueOf((int) (System.currentTimeMillis() - timeOfLastSuccessUpload));
      }

    });

    registry.register(name("uploads", "lastdurationmillis"), new Gauge<Integer>() {

      @Override
      public Integer getValue() {
        return lastUploadDuration;
      }

    });

    registry.register(name("uploaders", "expiring"), new Gauge<Integer>() {

      @Override
      public Integer getValue() {
        if (!expiring.isPresent()) {
          return 0;
        }

        return expiring.get().size();
      }

    });

    this.filesystemEventsMeter = registry.meter(name("filesystem", "events"));

    startJmxReporter();
  }

  private String name(String... names) {
    return MetricRegistry.name(SingularityS3UploaderMetrics.class, names);
  }

  public void setExpiringCollection(Collection<SingularityUploader> expiring) {
    this.expiring = Optional.of(expiring);
  }

  public void upload() {
    uploadCounter.inc();
    timeOfLastSuccessUpload = System.currentTimeMillis();
  }

  public void error() {
    errorCounter.inc();
  }

  private void startJmxReporter() {
    JmxReporter reporter = JmxReporter.forRegistry(registry).build();
    reporter.start();
  }

  public void startUploads() {
    uploadCounter.dec(uploadCounter.getCount());
    errorCounter.dec(errorCounter.getCount());
    startUploadsAt = System.nanoTime();
  }

  public void finishUploads() {
    long nanosElapsed = System.nanoTime() - startUploadsAt;
    lastUploadDuration = (int) TimeUnit.NANOSECONDS.toMillis(nanosElapsed);
  }

  public Counter getUploadCounter() {
    return uploadCounter;
  }

  public Counter getErrorCounter() {
    return errorCounter;
  }

  public Counter getUploaderCounter() {
    return uploaderCounter;
  }

  public Counter getImmediateUploaderCounter() {
    return immediateUploaderCounter;
  }

  public Timer getUploadTimer() {
    return uploadTimer;
  }

  public Meter getFilesystemEventsMeter() {
    return filesystemEventsMeter;
  }

}
