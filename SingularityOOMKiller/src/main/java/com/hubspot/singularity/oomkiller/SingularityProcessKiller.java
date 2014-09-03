package com.hubspot.singularity.oomkiller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.shared.Signal;

public class SingularityProcessKiller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityProcessKiller.class);

  public void sendSignal(String pid, Signal signal) {
    try {
      final long start = System.currentTimeMillis();

      LOG.info("Sending signal {} to {}", signal.name(), pid);

      final int exitCode = Runtime.getRuntime().exec(String.format("kill -%s %s", signal.getCode(), pid)).waitFor();

      LOG.info("Kill had exitCode {} after {}", exitCode, JavaUtils.duration(start));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public void killNow(String pid) {
    sendSignal(pid, Signal.SIGKILL);
  }

  public void requestExit(String pid) {
    sendSignal(pid, Signal.SIGTERM);
  }

}
