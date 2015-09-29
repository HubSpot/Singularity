package com.hubspot.singularity.data;

import java.util.Collections;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.api.ProtectACLCreateModePathAndBytesable;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
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

  public CuratorManager(CuratorFramework curator, SingularityConfiguration configuration) {
    this.configuration = configuration;
    this.curator = curator;
  }

  protected void log(String type, Optional<Integer> numItems, Optional<Integer> bytes, long start, String path) {
    final String message = String.format("%s (items: %s) (bytes: %s) in %s", type, numItems.or(1), bytes.or(0), path);

    if (bytes.isPresent() && bytes.get() > configuration.getDebugCuratorCallOverBytes()
        || System.currentTimeMillis() - start > configuration.getDebugCuratorCallOverMillis()) {
      LOG.debug(message);
    } else {
      LOG.trace(message);
    }
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
      log("Fetched children", Optional.of(numChildren), Optional.<Integer> absent(), start, root);
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
      log("Deleted", Optional.<Integer> absent(), Optional.<Integer> absent(), start, path);
    }
  }

  protected SingularityCreateResult create(String path) {
    return create(path, Optional.<byte[]> absent());
  }

  protected <T> SingularityCreateResult create(String path, T object, Transcoder<T> transcoder) {
    return create(path, Optional.of(transcoder.toBytes(object)));
  }

  protected SingularityCreateResult create(String path, Optional<byte[]> data) {
    final long start = System.currentTimeMillis();

    try {
      privateCreate(path, data);

      return SingularityCreateResult.CREATED;
    } catch (NodeExistsException nee) {
      return SingularityCreateResult.EXISTED;
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    } finally {
      log("Created", Optional.<Integer> absent(), Optional.<Integer> of(data.or(EMPTY_BYTES).length), start, path);
    }
  }

  private void privateCreate(String path, Optional<byte[]> data) throws Exception {
    ProtectACLCreateModePathAndBytesable<String> createBuilder = curator.create().creatingParentsIfNeeded();

    if (data.isPresent()) {
      createBuilder.forPath(path, data.get());
    } else {
      createBuilder.forPath(path);
    }

  }

  protected <T> SingularityCreateResult save(String path, T object, Transcoder<T> transcoder) {
    return save(path, Optional.of(transcoder.toBytes(object)));
  }

  protected SingularityCreateResult save(String path, Optional<byte[]> data) {
    final long start = System.currentTimeMillis();

    try {
      privateCreate(path, data);

      return SingularityCreateResult.CREATED;
    } catch (NodeExistsException nee) {
      return set(path, data);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    } finally {
      log("Saved", Optional.<Integer> absent(), Optional.<Integer> of(data.or(EMPTY_BYTES).length), start, path);
    }
  }

  protected SingularityCreateResult set(String path, Optional<byte[]> data) {
    final long start = System.currentTimeMillis();

    try {
      SetDataBuilder setDataBuilder = curator.setData();

      if (data.isPresent()) {
        setDataBuilder.forPath(path, data.get());
      } else {
        setDataBuilder.forPath(path);
      }

      return SingularityCreateResult.EXISTED;
    } catch (NoNodeException nne) {
      return save(path, data);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    } finally {
      log("Set", Optional.<Integer> absent(), Optional.<Integer> of(data.or(EMPTY_BYTES).length), start, path);
    }
  }

  protected <T> Optional<T> getData(String path, Optional<Stat> stat, Transcoder<T> transcoder) {
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

      return Optional.of(transcoder.fromBytes(data));
    } catch (NoNodeException nne) {
      return Optional.absent();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    } finally {
      log("Fetched", Optional.<Integer> absent(), Optional.<Integer> of(bytes), start, path);
    }
  }

  protected <T> Optional<T> getData(String path, Transcoder<T> transcoder) {
    return getData(path, Optional.<Stat> absent(), transcoder);
  }

  protected Optional<String> getStringData(String path) {
    return getData(path, StringTranscoder.INSTANCE);
  }

}
