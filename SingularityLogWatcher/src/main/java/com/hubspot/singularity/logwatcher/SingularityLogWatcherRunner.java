package com.hubspot.singularity.logwatcher;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Module;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherModule;
import com.hubspot.singularity.logwatcher.config.test.SingularityLogWatcherNoopModule;
import com.hubspot.singularity.logwatcher.config.test.SingularityLogWatcherTestModule;
import com.hubspot.singularity.logwatcher.impl.SingularityLogWatcherImplModule;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.shared.SingularityRunner;

public class SingularityLogWatcherRunner {

  public static void main(String... args) {
    new SingularityLogWatcherRunner().run(args);
  }

  private SingularityLogWatcherRunner() {}

  public void run(String[] args) {
    List<Module> modules = Lists.newArrayListWithCapacity(3);
    modules.add(new SingularityRunnerBaseModule(SingularityLogWatcherConfiguration.class));
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

    new SingularityRunner().run(modules);
  }

}
