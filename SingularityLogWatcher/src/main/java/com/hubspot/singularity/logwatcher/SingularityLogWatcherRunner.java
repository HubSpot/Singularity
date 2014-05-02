package com.hubspot.singularity.logwatcher;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherModule;
import com.hubspot.singularity.logwatcher.config.test.SingularityLogWatcherNoopModule;
import com.hubspot.singularity.logwatcher.config.test.SingularityLogWatcherTestModule;
import com.hubspot.singularity.logwatcher.driver.SingularityLogWatcherDriver;
import com.hubspot.singularity.logwatcher.impl.SingularityLogWatcherImplModule;

public class SingularityLogWatcherRunner {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityLogWatcherDriver.class);
  
  public static void main(String... args) {
    new SingularityLogWatcherRunner().run(args);
  }
  
  private SingularityLogWatcherRunner() {}
  
  public void run(String[] args) {
    List<Module> modules = Lists.newArrayListWithCapacity(2);
    modules.add(new SingularityLogWatcherModule());
    
    if (args.length > 0) {
      if (args[0].equals("--test")) {
        System.out.println("Using test module...");
        modules.add(new SingularityLogWatcherTestModule());
      } else if (args[0].equals("--noop")) {
        System.out.println("Using noop module...");
        modules.add(new SingularityLogWatcherNoopModule());
      }
    } else {
      modules.add(new SingularityLogWatcherImplModule());
    }
    
    final Injector injector = Guice.createInjector(modules);
    
    final SingularityLogWatcherDriver driver = injector.getInstance(SingularityLogWatcherDriver.class);
    
    Runtime.getRuntime().addShutdownHook(new Thread("SingularityLogWatcherGracefulShutdown") {

      @Override
      public void run() {
        driver.shutdown();
      }
      
    });
    
    try {
      driver.start();
    } catch (Throwable t) {
      LOG.error("Caught unexpected exception, exiting", t);
      driver.shutdown();
    }
  }

}
