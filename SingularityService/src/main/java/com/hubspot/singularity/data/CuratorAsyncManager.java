package com.hubspot.singularity.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

  private enum CuratorQueryMethod {
    GET_DATA(OperationType.GET_MULTI), CHECK_EXISTS(OperationType.CHECK_EXISTS), GET_CHILDREN(OperationType.GET_CHILDREN);

    private final OperationType operationType;

    private CuratorQueryMethod(OperationType operationType) {
      this.operationType = operationType;
    }

  }

  private <T> List<T> getAsyncChildrenThrows(final String parent, final Transcoder<T> transcoder) throws Exception {
    try {
      List<String> children = getChildren(parent);
      final List<String> paths = Lists.newArrayListWithCapacity(children.size());

      for (String child : children) {
        paths.add(ZKPaths.makePath(parent, child));
      }

      List<T> result = new ArrayList<>(getAsyncThrows(parent, paths, transcoder, Optional.absent()).values());


      return result;
    } catch (Throwable t) {
      throw t;
    }
  }

  private <T> Map<String, T> getAsyncThrows(final String pathNameForLogs, final Collection<String> paths, final Transcoder<T> transcoder, final Optional<ZkCache<T>> cache) throws Exception {
    final Map<String, T> objects = new HashMap<>(paths.size());

    if (cache.isPresent()) {
      for (Iterator<String> itr = paths.iterator(); itr.hasNext();) {
        final String path = itr.next();
        final Optional<T> fromCache = cache.get().get(path);
        if (fromCache.isPresent()) {
          objects.put(path, fromCache.get());
          itr.remove();
        }
      }
    }

    if (paths.isEmpty()) {
      return objects;
    }

    final Map<String, T> synchronizedObjects = Collections.synchronizedMap(objects);

    final CountDownLatch latch = new CountDownLatch(paths.size());
    final AtomicInteger bytes = new AtomicInteger();

    final BackgroundCallback callback = new BackgroundCallback() {

      @Override
      public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
        try {
          if (event.getData() == null || event.getData().length == 0) {
            LOG.trace("Expected active node {} but it wasn't there", event.getPath());
            return;
          }

          bytes.getAndAdd(event.getData().length);
          final T object = transcoder.fromBytes(event.getData());
          synchronizedObjects.put(event.getPath(), object);

          if (cache.isPresent()) {
            cache.get().set(event.getPath(), object);
          }
        } finally {
          latch.countDown();
        }
      }
    };

    return queryAndReturnResultsThrows(objects, paths, callback, latch, pathNameForLogs, bytes, CuratorQueryMethod.GET_DATA);
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
        try {
          if (event.getChildren() == null || event.getChildren().size() == 0) {
            LOG.trace("Expected children for node {} - but found none", event.getPath());
            return;
          }
          synchronizedObjects.addAll(Lists.transform(event.getChildren(), Transcoders.getFromStringFunction(idTranscoder)));
        } finally {
          latch.countDown();
        }
      }
    };

    return queryAndReturnResultsThrows(synchronizedObjects, parents, callback, latch, pathNameforLogs, new AtomicInteger(), CuratorQueryMethod.GET_CHILDREN);
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
        try {
          if (event.getStat() != null) {
            objects.add(Transcoders.getFromStringFunction(idTranscoder).apply(ZKPaths.getNodeFromPath(event.getPath())));
          }
        } finally {
          latch.countDown();
        }
      }
    };

    return queryAndReturnResultsThrows(objects, paths, callback, latch, pathNameforLogs, new AtomicInteger(), CuratorQueryMethod.GET_DATA);
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
        try {
          if (event.getStat() == null) {
            objects.add(pathsMap.get(event.getPath()));
          }
        } finally {
          latch.countDown();
        }
      }
    };

    return queryAndReturnResultsThrows(objects, pathsMap.keySet(), callback, latch, pathNameforLogs, new AtomicInteger(), CuratorQueryMethod.CHECK_EXISTS);
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
      return new ArrayList<>(getAsyncThrows(pathNameForLogs, paths, transcoder, Optional.of(cache)).values());
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  protected <T> List<T> getAsync(final String pathNameForLogs, final Collection<String> paths, final Transcoder<T> transcoder) {
    try {
      return new ArrayList<>(getAsyncThrows(pathNameForLogs, paths, transcoder, Optional.<ZkCache<T>> absent()).values());
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  protected <T> Map<String, T> getAsyncWithPath(final String pathNameForLogs, final Collection<String> paths, final Transcoder<T> transcoder) {
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

  protected <T, Q> Map<T, List<Q>> getAsyncNestedChildDataAsMapThrows(final String pathNameForLogs, final Map<String, T> parentPathsMap, final String subpath, final Transcoder<Q> transcoder) throws Exception {
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
        try {
          if (event.getData() == null || event.getData().length == 0) {
            LOG.trace("Expected active node {} but it wasn't there", event.getPath());
            return;
          }
          bytes.getAndAdd(event.getData().length);

          final Q object = transcoder.fromBytes(event.getData());

          if (allPathsMap.get(event.getPath()) != null) {
            resultsMap.putIfAbsent(allPathsMap.get(event.getPath()), new ArrayList<Q>());
            resultsMap.get(allPathsMap.get(event.getPath())).add(object);
          }
        } finally {
          latch.countDown();
        }
      }
    };

    return queryAndReturnResultsThrows(resultsMap, allPathsMap.keySet(), callback, latch, pathNameForLogs, bytes, CuratorQueryMethod.GET_DATA);
  }

  protected <T, Q> Map<T, List<Q>> getAsyncNestedChildDataAsMap(final String pathNameForLogs, final Map<String, T> parentPathsMap, final String subpath, final Transcoder<Q> transcoder) {
    try {
      return getAsyncNestedChildDataAsMapThrows(pathNameForLogs, parentPathsMap, subpath, transcoder);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private <T> T queryAndReturnResultsThrows(final T results, final Collection<String> paths, final BackgroundCallback callback, final CountDownLatch latch, final String pathNameForLogs, final AtomicInteger bytes, final CuratorQueryMethod method) throws Exception {
    final long start = System.currentTimeMillis();

    try {
      for (String path : paths) {
        switch (method) {
          case GET_DATA:
            curator.getData().inBackground(callback).forPath(path);
            break;
          case GET_CHILDREN:
            curator.getChildren().inBackground(callback).forPath(path);
            break;
          case CHECK_EXISTS:
          default:
            curator.checkExists().inBackground(callback).forPath(path);
            break;
        }
      }

      checkLatch(latch, pathNameForLogs);
    } finally {
      log(method.operationType, Optional.of(paths.size()), bytes.get() > 0 ? Optional.of(bytes.get()) : Optional.<Integer>absent(), start, pathNameForLogs);
    }

    return results;
  }

}
