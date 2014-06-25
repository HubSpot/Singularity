package com.hubspot.singularity.s3uploader;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;

public class SingularityS3UploaderMetrics {

  private final MetricRegistry registry;
  private final Counter uploaderCounter;
  private final Counter expiringUploaderCounter;
  private final Counter uploadCounter;
  private final Counter errorCounter;
  private final Timer uploadTimer;
  private final Meter filesystemEventsMeter;
  
  private long timeOfLastSuccessUpload;
  private int lastUploadDuration;
  private long startUploadsAt;
  
  @Inject
  public SingularityS3UploaderMetrics(MetricRegistry registry) {
    this.registry = registry;
    this.uploaderCounter = registry.counter(name("uploaders", "total"));
    this.expiringUploaderCounter = registry.counter(name("uploaders", "expiring"));
    this.uploadCounter = registry.counter(name("uploads", "success"));
    this.errorCounter = registry.counter(name("uploads", "errors"));
    this.uploadTimer = registry.timer(name("uploads", "timer"));
    
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
    
    this.filesystemEventsMeter = registry.meter(name("filesystem", "events"));
    
    startJmxReporter();
  }
  
  private String name(String... names) {
    return MetricRegistry.name(SingularityS3UploaderMetrics.class, names);
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
