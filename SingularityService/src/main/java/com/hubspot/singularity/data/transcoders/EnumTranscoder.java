package com.hubspot.singularity.data.transcoders;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;

public abstract class EnumTranscoder<T extends Enum<T>> implements Transcoder<T> {

  @Override
  public T transcode(byte[] data) throws SingularityJsonException {
    return fromString(new String(data, UTF_8));
  }

  protected abstract T fromString(String string);

  @Override
  public byte[] toBytes(T object) throws SingularityJsonException {
    return object.name().getBytes(UTF_8);
  }
}
