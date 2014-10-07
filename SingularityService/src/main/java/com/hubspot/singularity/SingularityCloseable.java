package com.hubspot.singularity;

import java.util.concurrent.ExecutorService;

import com.google.common.base.Optional;

public abstract class SingularityCloseable<T extends ExecutorService> {

  private final SingularityCloser closer;

  public SingularityCloseable(SingularityCloser closer) {
    this.closer = closer;
  }

  public void close() {
    final Optional<T> executorService = getExecutorService();
    if (!executorService.isPresent()) {
      return;
    }

    closer.shutdown(getClass().getSimpleName(), executorService.get());
  }

  public abstract Optional<T> getExecutorService();

}
