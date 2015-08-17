package com.hubspot.singularity.data;

import static com.google.common.base.Preconditions.checkState;

import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger;
import org.apache.curator.retry.RetryOneTime;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.inject.Inject;

import io.dropwizard.lifecycle.Managed;

@Singleton
public class ExecutorIdGenerator implements Managed {

  private volatile DistributedAtomicInteger distributedGenerator = null;
  private final char[] alphabet;

  private static final String COUNTER_PATH = "/executors/counter";

  private final CuratorFramework curator;

  @Inject
  public ExecutorIdGenerator(CuratorFramework curator) {
    this.curator = curator;
    this.alphabet = buildAlphabet();
  }

  @Override
  public void start() {
    this.distributedGenerator = new DistributedAtomicInteger(curator, COUNTER_PATH, new RetryOneTime(1));
  }

  @Override
  public void stop() {
  }

  public String getNextExecutorId() {
    checkState(distributedGenerator != null, "never started!");
    try {
      AtomicValue<Integer> atomic = distributedGenerator.increment();
      Preconditions.checkState(atomic.succeeded(), "Atomic increment did not succeed");
      return convertUsingAlphabet(atomic.postValue());
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private String convertUsingAlphabet(int number) {
    final StringBuilder bldr = new StringBuilder();

    while (number > 0) {
      int remainder = number % alphabet.length;
      bldr.append(alphabet[remainder]);
      number = number / alphabet.length;
    }

    return bldr.toString();
  }

  private char[] buildAlphabet() {
    final char[] alphabet = new char[36];
    int c = 0;

    // add integers
    for (int i = 48; i < 58; i++) {
      alphabet[c++] = (char) i;
    }

    // add letters
    for (int i = 97; i < 123; i++) {
      alphabet[c++] = (char) i;
    }

    return alphabet;
  }

}
