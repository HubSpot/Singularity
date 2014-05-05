package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.RetryOneTime;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;

public class ExecutorIdGenerator {

  private final DistributedAtomicLong distributedGenerator;
  private final BaseEncoding encoder;
  
  private static final String COUNTER_PATH = "/executors/counter";
  
  @Inject
  public ExecutorIdGenerator(CuratorFramework curator) {
    this.distributedGenerator = new DistributedAtomicLong(curator, COUNTER_PATH, new RetryOneTime(1));
    this.encoder = BaseEncoding.base64Url().omitPadding().lowerCase();
  }
  
  public String getNextExecutorId() {
    try {
      AtomicValue<Long> atomic = distributedGenerator.increment();
      Preconditions.checkState(atomic.succeeded(), "Atomic increment did not succeed");
      return encoder.encode(Longs.toByteArray(atomic.postValue()));
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
}
