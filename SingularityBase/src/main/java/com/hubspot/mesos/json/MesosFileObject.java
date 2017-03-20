package com.hubspot.mesos.json;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosFileObject {
  private final String gid;
  private final String mode;
  private final long mtime;
  private final int nlink;
  private final String path;
  private final long size;
  private final String uid;

  @JsonCreator
  public MesosFileObject(@JsonProperty("gid") String gid, @JsonProperty("mode") String mode,
                         @JsonProperty("mtime") long mtime, @JsonProperty("nlink") int nlink,
                         @JsonProperty("path") String path, @JsonProperty("size") long size,
                         @JsonProperty("uid") String uid) {
    this.gid = gid;
    this.mode = mode;
    this.mtime = mtime;
    this.nlink = nlink;
    this.path = path;
    this.size = size;
    this.uid = uid;
  }

  public String getGid() {
    return gid;
  }

  public String getMode() {
    return mode;
  }

  public long getMtime() {
    return mtime;
  }

  public int getNlink() {
    return nlink;
  }

  public String getPath() {
    return path;
  }

  public long getSize() {
    return size;
  }

  public String getUid() {
    return uid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MesosFileObject that = (MesosFileObject) o;
    return mtime == that.mtime &&
        nlink == that.nlink &&
        size == that.size &&
        Objects.equals(gid, that.gid) &&
        Objects.equals(mode, that.mode) &&
        Objects.equals(path, that.path) &&
        Objects.equals(uid, that.uid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gid, mode, mtime, nlink, path, size, uid);
  }

  @Override
  public String toString() {
    return "MesosFileObject{" +
        "gid='" + gid + '\'' +
        ", mode='" + mode + '\'' +
        ", mtime=" + mtime +
        ", nlink=" + nlink +
        ", path='" + path + '\'' +
        ", size=" + size +
        ", uid='" + uid + '\'' +
        '}';
  }
}
