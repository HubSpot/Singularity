package com.hubspot.singularity.runner.base.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public abstract class BaseRunnerConfiguration implements OverridableByProperty {
  @JsonProperty("logging")
  protected SingularityRunnerBaseLoggingConfiguration logging = new SingularityRunnerBaseLoggingConfiguration();

  protected BaseRunnerConfiguration(Optional<String> rootLogName) {
    this.logging.setFilename(rootLogName);
  }

  public SingularityRunnerBaseLoggingConfiguration getLogging() {
    return logging;
  }

  public void setLogging(SingularityRunnerBaseLoggingConfiguration logging) {
    this.logging = logging;
  }
}
