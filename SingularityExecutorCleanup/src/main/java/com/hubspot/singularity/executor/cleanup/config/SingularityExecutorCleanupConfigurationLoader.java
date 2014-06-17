package com.hubspot.singularity.executor.cleanup.config;

import java.util.Properties;

import com.hubspot.singularity.runner.base.config.SingularityConfigurationLoader;

public class SingularityExecutorCleanupConfigurationLoader extends SingularityConfigurationLoader {

  public static final String SAFE_MODE_WONT_RUN_WITH_NO_TASKS = "executor.cleanup.safe.mode.wont.run.with.no.tasks";
  
  public SingularityExecutorCleanupConfigurationLoader() {
    super("/etc/singularity.executor.cleanup.properties");
  }

  public void bindDefaults(Properties properties) {
    properties.put(SAFE_MODE_WONT_RUN_WITH_NO_TASKS, Boolean.toString(true));
  }
  
}
