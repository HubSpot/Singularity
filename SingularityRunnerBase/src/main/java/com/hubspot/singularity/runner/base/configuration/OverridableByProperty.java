package com.hubspot.singularity.runner.base.configuration;

import java.util.Properties;

public interface OverridableByProperty {
  void updateFromProperties(Properties properties);
}
