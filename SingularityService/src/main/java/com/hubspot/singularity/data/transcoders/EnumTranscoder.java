package com.hubspot.singularity.data.transcoders;

import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;

public abstract class EnumTranscoder<T extends Enum<T>> implements Transcoder<T> {

  @Override
  public T transcode(byte[] data) throws SingularityJsonException {
    String string = JavaUtils.toString(data);
    return fromString(string);
  }

  protected abstract T fromString(String string);

  @Override
  public byte[] toBytes(T object) throws SingularityJsonException {
    return JavaUtils.toBytes(object.name());
  }



}
