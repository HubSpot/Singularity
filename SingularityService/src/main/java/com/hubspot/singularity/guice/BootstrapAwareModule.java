package com.hubspot.singularity.guice;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.inject.Binder;
import com.google.inject.Module;

import io.dropwizard.setup.Bootstrap;

public abstract class BootstrapAwareModule implements Module {
  private volatile Bootstrap<?> bootstrap = null;

  @Override
  public void configure(Binder binder) {
    configure(binder, getBootstrap());
  }

  protected Bootstrap<?> getBootstrap() {
    return checkNotNull(this.bootstrap, "bootstrap was not set!");
  }

  public void setBootstrap(Bootstrap<?> bootstrap) {
    checkState(this.bootstrap == null, "bootstrap was already set!");
    this.bootstrap = checkNotNull(bootstrap, "bootstrap is null");
  }

  protected abstract void configure(Binder binder, Bootstrap<?> bootstrap);
}
