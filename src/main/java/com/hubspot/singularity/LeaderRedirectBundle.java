package com.hubspot.singularity;

import com.codahale.dropwizard.Bundle;
import com.codahale.dropwizard.setup.Bootstrap;
import com.codahale.dropwizard.setup.Environment;

public class LeaderRedirectBundle implements Bundle {

  @Override
  public void initialize(Bootstrap<?> bootstrap) {

  }

  @Override
  public void run(Environment environment) {
    environment.jersey().getResourceConfig().getResourceFilterFactories().add(LeaderRedirectorFilterFactory.class);
  }
}
