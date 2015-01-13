package com.hubspot.singularity.mesos;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.apache.mesos.SchedulerDriver;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;

public class SchedulerDriverSupplier implements Supplier<Optional<SchedulerDriver>> {

  private final AtomicReference<SchedulerDriver> driverHolder = new AtomicReference<>();

  @Inject
  public SchedulerDriverSupplier() {}

  public void setSchedulerDriver(SchedulerDriver schedulerDriver) {
    driverHolder.set(schedulerDriver);
  }

  @Override
  public Optional<SchedulerDriver> get() {
    return Optional.fromNullable(driverHolder.get());
  }

}
