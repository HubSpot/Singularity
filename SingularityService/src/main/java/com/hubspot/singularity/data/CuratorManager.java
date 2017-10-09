package com.hubspot.singularity.data;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.api.ProtectACLCreateModePathAndBytesable;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.StringTranscoder;
import com.hubspot.singularity.data.transcoders.Transcoder;

public abstract class CuratorManager {

  private static final Logger LOG = LoggerFactory.getLogger(CuratorManager.class);
  private static final byte[] EMPTY_BYTES = new byte[0];

  protected final SingularityConfiguration configuration;
  protected final CuratorFramework curator;

  private final Map<OperationType, Metrics> typeToMetrics;

  public CuratorManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry) {
    this.configuration = configuration;
    this.curator = curator;

    typeToMetrics = ImmutableMap.<OperationType, Metrics> builder()
        .put(OperationType.GET_MULTI, new Metrics(metricRegistry, OperationType.GET_MULTI))
        .put(OperationType.GET, new Metrics(metricRegistry, OperationType.GET))
        .put(OperationType.CHECK_EXISTS, new Metrics(metricRegistry, OperationType.CHECK_EXISTS))
        .put(OperationType.GET_CHILDREN, new Metrics(metricRegistry, OperationType.GET_CHILDREN))
        .put(OperationType.DELETE, new Metrics(metricRegistry, OperationType.DELETE))
        .put(OperationType.WRITE, new Metrics(metricRegistry, OperationType.WRITE)).build();
  }

  public enum OperationType {
    GET_MULTI, GET, CHECK_EXISTS, GET_CHILDREN, DELETE, WRITE;
  }

  private static class Metrics {

    private final Meter bytesMeter;
    private final Meter itemsMeter;
    private final Timer timer;

    public Metrics(MetricRegistry registry, OperationType type) {
      this.bytesMeter = registry.meter(String.format("zk.bytes.%s", type.name().toLowerCase()));
      this.itemsMeter = registry.meter(String.format("zk.items.%s", type.name().toLowerCase()));
      this.timer = registry.timer(String.format("zk.%s", type.name().toLowerCase()));
    }
  }

  protected void log(OperationType type, Optional<Integer> numItems, Optional<Integer> bytes, long start, String path) {
    final String message = String.format("%s (items: %s) (bytes: %s) in %s (%s)", type.name(), numItems.or(1), bytes.or(0), JavaUtils.duration(start), path);

    final long duration = System.currentTimeMillis() - start;

    if (bytes.isPresent() && bytes.get() > configuration.getDebugCuratorCallOverBytes()
        || System.currentTimeMillis() - start > configuration.getDebugCuratorCallOverMillis()) {
      LOG.debug(message);
    } else {
      LOG.trace(message);
    }

    Metrics metrics = typeToMetrics.get(type);

    if (bytes.isPresent()) {
      metrics.bytesMeter.mark(bytes.get());
    }

    metrics.itemsMeter.mark(numItems.or(1));
    metrics.timer.update(duration, TimeUnit.MILLISECONDS);
  }

  protected int getNumChildren(String path) {
    try {
      Stat s = curator.checkExists().forPath(path);
      if (s != null) {
        return s.getNumChildren();
      }
    } catch (NoNodeException nne) {
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }

    return 0;
  }

  protected Optional<Stat> checkExists(String path) {
    try {
      Stat stat = curator.checkExists().forPath(path);
      return Optional.fromNullable(stat);
    } catch (NoNodeException nne) {
      return Optional.absent();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      return Optional.absent();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  protected boolean exists(String path) {
    return checkExists(path).isPresent();
  }

  protected List<String> getChildren(String root) {
    LOG.trace("Preparing to call getChildren() on {}", root);
    final long start = System.currentTimeMillis();
    int numChildren = 0;

    try {
      final List<String> children = curator.getChildren().forPath(root);
      numChildren = children.size();

      return children;
    } catch (NoNodeException nne) {
      return Collections.emptyList();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    } finally {
      log(OperationType.GET_CHILDREN, Optional.of(numChildren), Optional.absent(), start, root);
    }
  }

  protected SingularityDeleteResult delete(String path) {
    final long start = System.currentTimeMillis();

    try {
      curator.delete().deletingChildrenIfNeeded().forPath(path);
      return SingularityDeleteResult.DELETED;
    } catch (NoNodeException nne) {
      LOG.trace("Tried to delete an item at path {} that didn't exist", path);
      return SingularityDeleteResult.DIDNT_EXIST;
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    } finally {
      log(OperationType.DELETE, Optional.absent(), Optional.<Integer> absent(), start, path);
    }
  }

  protected SingularityCreateResult create(String path) {
    return create(path, Optional.<byte[]>absent());
  }

  protected <T> SingularityCreateResult create(String path, T object, Transcoder<T> transcoder) {
    return create(path, Optional.of(transcoder.toBytes(object)));
  }

  protected SingularityCreateResult create(String path, Optional<byte[]> data) {
    try {
      privateCreate(path, data);

      return SingularityCreateResult.CREATED;
    } catch (NodeExistsException nee) {
      return SingularityCreateResult.EXISTED;
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private void privateCreate(String path, Optional<byte[]> data) throws Exception {
    final long start = System.currentTimeMillis();

    try {
      ProtectACLCreateModePathAndBytesable<String> createBuilder = curator.create().creatingParentsIfNeeded();


      if (data.isPresent()) {
        createBuilder.forPath(path, data.get());
      } else {
        createBuilder.forPath(path);
      }
    } finally {
      log(OperationType.WRITE, Optional.absent(), Optional.of(data.or(EMPTY_BYTES).length), start, path);
    }
  }

  protected <T> SingularityCreateResult save(String path, T object, Transcoder<T> transcoder) {
    return save(path, Optional.of(transcoder.toBytes(object)));
  }

  protected SingularityCreateResult save(String path, Optional<byte[]> data) {
    try {
      privateCreate(path, data);

      return SingularityCreateResult.CREATED;
    } catch (NodeExistsException nee) {
      return set(path, data);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private void privateSet(String path, Optional<byte[]> data) throws Exception {
    final long start = System.currentTimeMillis();

    try {
      SetDataBuilder setDataBuilder = curator.setData();

      if (data.isPresent()) {
        setDataBuilder.forPath(path, data.get());
      } else {
        setDataBuilder.forPath(path);
      }
    } finally {
      log(OperationType.WRITE, Optional.<Integer>absent(), Optional.<Integer>of(data.or(EMPTY_BYTES).length), start, path);
    }
  }

  protected <T> SingularityCreateResult set(String path, T object, Transcoder<T> transcoder) {
    return set(path, Optional.of(transcoder.toBytes(object)));
  }

  protected SingularityCreateResult set(String path, Optional<byte[]> data) {
    try {
      privateSet(path, data);

      return SingularityCreateResult.EXISTED;
    } catch (NoNodeException nne) {
      return save(path, data);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private <T> Optional<T> getData(String path, Optional<Stat> stat, Transcoder<T> transcoder, Optional<ZkCache<T>> zkCache, Optional<Boolean> shouldCheckExists) {
    if (!stat.isPresent() && zkCache.isPresent()) {
      Optional<T> cachedValue = zkCache.get().get(path);
      if (cachedValue.isPresent() && (!shouldCheckExists.isPresent() || (shouldCheckExists.get().booleanValue() && checkExists(path).isPresent()))) {
        return cachedValue;
      }
    }

    final long start = System.currentTimeMillis();
    int bytes = 0;

    try {
      GetDataBuilder bldr = curator.getData();

      if (stat.isPresent()) {
        bldr.storingStatIn(stat.get());
      }

      byte[] data = bldr.forPath(path);

      if (data == null || data.length == 0) {
        LOG.trace("Empty data found for path {}", path);
        return Optional.absent();
      }

      bytes = data.length;

      final T object = transcoder.fromBytes(data);

      if (zkCache.isPresent()) {
        zkCache.get().set(path, object);
      }

      return Optional.of(object);
    } catch (NoNodeException nne) {
      LOG.trace("No node found for path {}", path);
      return Optional.absent();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    } finally {
      log(OperationType.GET, Optional.absent(), Optional.<Integer> of(bytes), start, path);
    }
  }

  protected <T> Optional<T> getData(String path, Transcoder<T> transcoder) {
    return getData(path, Optional.<Stat>absent(), transcoder, Optional.<ZkCache<T>>absent(), Optional.<Boolean>absent());
  }

  protected <T> Optional<T> getData(String path, Transcoder<T> transcoder, ZkCache<T> zkCache, boolean shouldCheckExists) {
    return getData(path, Optional.<Stat>absent(), transcoder, Optional.of(zkCache), Optional.of(shouldCheckExists));
  }

  protected Optional<String> getStringData(String path) {
    return getData(path, Optional.<Stat>absent(), StringTranscoder.INSTANCE, Optional.<ZkCache<String>>absent(), Optional.<Boolean>absent());
  }
}
