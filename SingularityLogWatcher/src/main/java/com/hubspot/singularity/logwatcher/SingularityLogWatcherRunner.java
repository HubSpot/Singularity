package com.hubspot.singularity.logwatcher;

import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hubspot.singularity.logwatcher.config.test.SingularityLogWatcherTestModule;
import com.hubspot.singularity.logwatcher.tailer.SingularityLogWatcherTailer;

public class SingularityLogWatcherRunner {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityLogWatcherRunner.class);

  public static void main(String... args) {
    new SingularityLogWatcherRunner().run(args[0]);
  }
  
  private SingularityLogWatcherRunner() {}
  
  public void run(String logfile) {
    final Injector injector = Guice.createInjector(new SingularityLogWatcherTestModule());
    
    SingularityLogWatcherTailer tailer = new SingularityLogWatcherTailer("test-tag", Paths.get(logfile), 8192, 20L, injector.getInstance(SimpleStore.class), injector.getInstance(LogForwarder.class));
    
    try {
      tailer.watch();
    } catch (Exception e) {
      LOG.error("While watching {}", logfile, e);
    }
  }

}
