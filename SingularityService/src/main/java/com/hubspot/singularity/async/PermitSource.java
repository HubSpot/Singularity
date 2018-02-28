package com.hubspot.singularity.async;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

class PermitSource {
  private final com.google.common.base.Supplier<Integer> concurrentRequestLimit;
  private final AtomicInteger concurrentRequests;

  public PermitSource(Supplier<Integer> concurrentRequestLimit) {
    this.concurrentRequestLimit = Suppliers.memoizeWithExpiration(concurrentRequestLimit::get, 1, TimeUnit.MINUTES);;
    this.concurrentRequests = new AtomicInteger();
  }

  public boolean tryAcquire() {
    int highWatermark = concurrentRequestLimit.get();
    while (true) {
      int oldValue = concurrentRequests.get();
      if (oldValue >= highWatermark) {
        return false;
      } else if (concurrentRequests.compareAndSet(oldValue, oldValue + 1)) {
        return true;
      }
    }
  }

  public void release() {
    concurrentRequests.decrementAndGet();
  }
}
