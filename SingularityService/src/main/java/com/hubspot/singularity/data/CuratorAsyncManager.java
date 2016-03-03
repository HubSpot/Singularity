package com.hubspot.singularity.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hubspot.singularity.SingularityId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.IdTranscoder;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.data.transcoders.Transcoders;

public abstract class CuratorAsyncManager extends CuratorManager {

  private static final Logger LOG = LoggerFactory.getLogger(CuratorAsyncManager.class);

  public CuratorAsyncManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry) {
    super(curator, configuration, metricRegistry);
  }

  private <T> List<T> getAsyncChildrenThrows(final String parent, final Transcoder<T> transcoder) throws Exception {
    final List<String> children = getChildren(parent);
    final List<String> paths = Lists.newArrayListWithCapacity(children.size());

    for (String child : children) {
      paths.add(ZKPaths.makePath(parent, child));
    }

    return getAsyncThrows(parent, paths, transcoder, Optional.<ZkCache<T>> absent());
  }

  private <T> List<T> getAsyncThrows(final String pathNameForLogs, final Collection<String> paths, final Transcoder<T> transcoder, final Optional<ZkCache<T>> cache) throws Exception {
    final List<T> objects = new ArrayList<>(paths.size());

    if (cache.isPresent()) {
      for (Iterator<String> itr = paths.iterator(); itr.hasNext();) {
        Optional<T> fromCache = cache.get().get(itr.next());
        if (fromCache.isPresent()) {
          objects.add(fromCache.get());
          itr.remove();
        }
      }
    }

    if (paths.isEmpty()) {
      return objects;
    }

    final List<T> synchronizedObjects = Collections.synchronizedList(objects);

    final CountDownLatch latch = new CountDownLatch(paths.size());
    final AtomicInteger bytes = new AtomicInteger();

    final BackgroundCallback callback = new BackgroundCallback() {

      @Override
      public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
        if (event.getData() == null || event.getData().length == 0) {
          LOG.trace("Expected active node {} but it wasn't there", event.getPath());

          latch.countDown();

          return;
        }

        bytes.getAndAdd(event.getData().length);

        final T object = transcoder.fromBytes(event.getData());

        synchronizedObjects.add(object);

        if (cache.isPresent()) {
          cache.get().set(event.getPath(), object);
        }

        latch.countDown();
      }
    };

    final long start = System.currentTimeMillis();

    try {
      for (String path : paths) {
        curator.getData().inBackground(callback).forPath(path);
      }

      checkLatch(latch, pathNameForLogs);
    } finally {
      log(OperationType.READ, Optional.<Integer> of(objects.size()), Optional.<Integer> of(bytes.get()), start, pathNameForLogs);
    }

    return objects;
  }

  private void checkLatch(CountDownLatch latch, String path) throws InterruptedException {
    if (!latch.await(configuration.getZookeeperAsyncTimeout(), TimeUnit.MILLISECONDS)) {
      throw new IllegalStateException(String.format("Timed out waiting response for objects from %s, waited %s millis", path, configuration.getZookeeperAsyncTimeout()));
    }
  }

  private <T extends SingularityId> List<T> getChildrenAsIdsForParentsThrows(final String pathNameforLogs, final Collection<String> parents, final IdTranscoder<T> idTranscoder) throws Exception {
    if (parents.isEmpty()) {
      return Collections.emptyList();
    }

    final List<T> objects = Lists.newArrayListWithExpectedSize(parents.size());
    final List<T> synchronizedObjects = Collections.synchronizedList(objects);

    final CountDownLatch latch = new CountDownLatch(parents.size());

    final BackgroundCallback callback = new BackgroundCallback() {

      @Override
      public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
        if (event.getChildren() == null || event.getChildren().size() == 0) {
          LOG.trace("Expected children for node {} - but found none", event.getPath());

          latch.countDown();

          return;
        }

        synchronizedObjects.addAll(Lists.transform(event.getChildren(), Transcoders.getFromStringFunction(idTranscoder)));

        latch.countDown();
      }
    };

    final long start = System.currentTimeMillis();

    try {
      for (String parent : parents) {
        curator.getChildren().inBackground(callback).forPath(parent);
      }

      checkLatch(latch, pathNameforLogs);
    } finally {
      log(OperationType.READ, Optional.<Integer> of(objects.size()), Optional.<Integer> absent(), start, pathNameforLogs);
    }

    return objects;
  }

  protected <T extends SingularityId> List<T> getChildrenAsIdsForParents(final String pathNameforLogs, final Collection<String> parents, final IdTranscoder<T> idTranscoder) {
    try {
      return getChildrenAsIdsForParentsThrows(pathNameforLogs, parents, idTranscoder);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  protected <T extends SingularityId> List<T> getChildrenAsIds(final String rootPath, final IdTranscoder<T> idTranscoder) {
    return Lists.transform(getChildren(rootPath), Transcoders.getFromStringFunction(idTranscoder));
  }

  private <T extends SingularityId> List<T> existsThrows(final String pathNameforLogs, final Collection<String> paths, final IdTranscoder<T> idTranscoder) throws Exception {
    if (paths.isEmpty()) {
      return Collections.emptyList();
    }

    final List<T> objects = Lists.newArrayListWithCapacity(paths.size());

    final CountDownLatch latch = new CountDownLatch(paths.size());

    final BackgroundCallback callback = new BackgroundCallback() {

      @Override
      public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
        if (event.getStat() == null) {
          latch.countDown();

          return;
        }

        objects.add(Transcoders.getFromStringFunction(idTranscoder).apply(ZKPaths.getNodeFromPath(event.getPath())));

        latch.countDown();
      }
    };

    final long start = System.currentTimeMillis();

    try {
      for (String path : paths) {
        curator.checkExists().inBackground(callback).forPath(path);
      }

      checkLatch(latch, pathNameforLogs);
    } finally {
      log(OperationType.READ, Optional.<Integer> of(objects.size()), Optional.<Integer> absent(), start, pathNameforLogs);
    }

    return objects;
  }

  protected <T extends SingularityId> List<T> exists(final String pathNameForLogs, final Collection<String> paths, final IdTranscoder<T> idTranscoder) {
    try {
      return existsThrows(pathNameForLogs, paths, idTranscoder);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private <T extends SingularityId> List<T> notExistsThrows(final String pathNameforLogs, final Map<String, T> pathsMap) throws Exception {
    if (pathsMap.isEmpty()) {
      return Collections.emptyList();
    }

    final List<T> objects = Lists.newArrayListWithCapacity(pathsMap.size());

    final CountDownLatch latch = new CountDownLatch(pathsMap.size());

    final BackgroundCallback callback = new BackgroundCallback() {

      @Override
      public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
        if (event.getStat() == null) {
          objects.add(pathsMap.get(event.getPath()));
        }
        latch.countDown();
      }
    };

    final long start = System.currentTimeMillis();

    try {
      for (String path : pathsMap.keySet()) {
        curator.checkExists().inBackground(callback).forPath(path);
      }

      checkLatch(latch, pathNameforLogs);
    } finally {
      log(OperationType.READ, Optional.<Integer> of(objects.size()), Optional.<Integer> absent(), start, pathNameforLogs);
    }

    return objects;
  }

  protected <T extends SingularityId> List<T> notExists(final String pathNameForLogs, final Map<String, T> pathsMap) {
    try {
      return notExistsThrows(pathNameForLogs, pathsMap);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  protected <T> List<T> getAsync(final String pathNameForLogs, final Collection<String> paths, final Transcoder<T> transcoder, final ZkCache<T> cache) {
    try {
      return getAsyncThrows(pathNameForLogs, paths, transcoder, Optional.of(cache));
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  protected <T> List<T> getAsync(final String pathNameForLogs, final Collection<String> paths, final Transcoder<T> transcoder) {
    try {
      return getAsyncThrows(pathNameForLogs, paths, transcoder, Optional.<ZkCache<T>> absent());
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  protected <T> List<T> getAsyncChildren(final String parent, final Transcoder<T> transcoder) {
    try {
      return getAsyncChildrenThrows(parent, transcoder);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  protected <T, Q> Map<T, List<Q>> getAsynchNestedChildrenAsMapThrows(final String pathNameForLogs, final Map<String, T> parentPathsMap, final String subpath, final Transcoder<Q> transcoder) throws Exception {
    final Map<String, T> allPathsMap = Maps.newHashMap();
    for (Map.Entry<String, T> entry : parentPathsMap.entrySet()) {
      for (String child : getChildren(ZKPaths.makePath(entry.getKey(), subpath))) {
        allPathsMap.put(ZKPaths.makePath(entry.getKey(), subpath, child), entry.getValue());
      }
    }

    final ConcurrentHashMap<T, List<Q>> resultsMap = new ConcurrentHashMap<>();
    final CountDownLatch latch = new CountDownLatch(allPathsMap.size());
    final AtomicInteger bytes = new AtomicInteger();
    final BackgroundCallback callback = new BackgroundCallback() {

      @Override
      public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
        if (event.getData() == null || event.getData().length == 0) {
          LOG.trace("Expected active node {} but it wasn't there", event.getPath());
          latch.countDown();
          return;
        }

        bytes.getAndAdd(event.getData().length);

        final Q object = transcoder.fromBytes(event.getData());

        if (allPathsMap.get(event.getPath()) != null) {
          resultsMap.putIfAbsent(allPathsMap.get(event.getPath()), new ArrayList<Q>());
          resultsMap.get(allPathsMap.get(event.getPath())).add(object);
        }

        latch.countDown();
      }
    };

    final long start = System.currentTimeMillis();

    try {
      for (String path : allPathsMap.keySet()) {
        curator.getData().inBackground(callback).forPath(path);
      }

      checkLatch(latch, pathNameForLogs);
    } finally {
      log(OperationType.READ, Optional.of(resultsMap.size()), Optional.of(bytes.get()), start, pathNameForLogs);
    }

    return resultsMap;
  }

  protected <T, Q> Map<T, List<Q>> getAsynchNestedChildrenAsMap(final String pathNameForLogs, final Map<String, T> parentPathsMap, final String subpath, final Transcoder<Q> transcoder) {
    try {
      return getAsynchNestedChildrenAsMapThrows(pathNameForLogs, parentPathsMap, subpath, transcoder);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

}

