package com.hubspot.singularity.runner.base.shared;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import com.hubspot.mesos.JavaUtils;

public abstract class WatchServiceHelper implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(WatchServiceHelper.class);

  private final WatchService watchService;
  private final Path watchDirectory;
  private final long pollWaitCheckShutdownMillis;
  private final List<WatchEvent.Kind<Path>> watchEvents;

  private volatile boolean stopped;

  public WatchServiceHelper(long pollWaitCheckShutdownMillis, Path watchDirectory, List<WatchEvent.Kind<Path>> watchEvents) {
    this.pollWaitCheckShutdownMillis = pollWaitCheckShutdownMillis;

    this.watchDirectory = watchDirectory;
    this.watchEvents = watchEvents;
    this.watchService = createWatchService();

    this.stopped = false;
  }

  public boolean stop() {
    if (stopped) {
      return false;
    }
    stopped = true;
    return true;
  }

  public boolean isStopped() {
    return stopped;
  }

  public void setStopped(boolean stopped) {
    this.stopped = stopped;
  }

  @Override
  public void close() {
    try {
      Closeables.close(watchService, true);
    } catch (IOException ioe) {
      // impossible!
    }
  }

  private WatchService createWatchService() {
    try {
      return FileSystems.getDefault().newWatchService();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public void watch() throws IOException, InterruptedException {
    LOG.info("Watching directory {} for event(s) {}", watchDirectory, watchEvents);

    WatchKey watchKey = watchDirectory.register(watchService, watchEvents.toArray(new WatchEvent.Kind[watchEvents.size()]));

    while (!stopped) {
      if (watchKey != null) {

        processWatchKey(watchKey);

        if (!watchKey.reset()) {
          LOG.warn("WatchKey for {} no longer valid", watchDirectory);
          break;
        }
      }

      watchKey = watchService.poll(pollWaitCheckShutdownMillis, TimeUnit.MILLISECONDS);
    }
  }

  protected abstract boolean processEvent(WatchEvent.Kind<?> kind, Path filename) throws IOException;

  @SuppressWarnings("unchecked")
  private WatchEvent<Path> cast(WatchEvent<?> event) {
    return (WatchEvent<Path>) event;
  }

  private void processWatchKey(WatchKey watchKey) throws IOException {
    final long start = System.currentTimeMillis();
    final List<WatchEvent<?>> events = watchKey.pollEvents();

    int processed = 0;

    for (WatchEvent<?> event : events) {
      WatchEvent.Kind<?> kind = event.kind();

      if (!watchEvents.contains(kind)) {
        LOG.trace("Ignoring an {} event to {}", event.context());
        continue;
      }

      WatchEvent<Path> ev = cast(event);
      Path filename = ev.context();

      if (processEvent(kind, filename)) {
        processed++;
      }
    }

    LOG.debug("Handled {} out of {} event(s) for {} in {}", processed, events.size(), watchDirectory, JavaUtils.duration(start));
  }

}
