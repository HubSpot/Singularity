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
import com.hubspot.singularity.data.transcoders.StringTranscoder;
import com.hubspot.singularity.data.transcoders.Transcoder;

public abstract class CuratorManager {

  private static final Logger LOG = LoggerFactory.getLogger(CuratorManager.class);

  protected final CuratorFramework curator;

  public CuratorManager(CuratorFramework curator) {
    this.curator = curator;
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
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }

    return Optional.absent();
  }

  protected boolean exists(String path) {
    return checkExists(path).isPresent();
  }

  protected List<String> getChildren(String root) {
    try {
      return curator.getChildren().forPath(root);
    } catch (NoNodeException nne) {
      return Collections.emptyList();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  protected SingularityDeleteResult delete(String path) {
    try {
      curator.delete().deletingChildrenIfNeeded().forPath(path);
      return SingularityDeleteResult.DELETED;
    } catch (NoNodeException nne) {
      LOG.trace("Tried to delete an item at path {} that didn't exist", path);
      return SingularityDeleteResult.DIDNT_EXIST;
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  protected SingularityCreateResult create(String path) {
    return create(path, Optional.<byte[]> absent());
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
    try {
      privateCreate(path, data);

      return SingularityCreateResult.CREATED;
    } catch (NodeExistsException nee) {
      return set(path, data);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  protected SingularityCreateResult set(String path, Optional<byte[]> data) {
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
    }
  }

  protected <T> Optional<T> getData(String path, Optional<Stat> stat, Transcoder<T> transcoder) {
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

      return Optional.of(transcoder.fromBytes(data));
    } catch (NoNodeException nne) {
      return Optional.absent();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  protected <T> Optional<T> getData(String path, Transcoder<T> transcoder) {
    return getData(path, Optional.<Stat> absent(), transcoder);
  }

  protected Optional<String> getStringData(String path) {
    return getData(path, StringTranscoder.INSTANCE);
  }

}
