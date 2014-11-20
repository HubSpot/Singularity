package com.hubspot.singularity.data.transcoders;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.annotation.Nullable;

public abstract class EnumTranscoder<T extends Enum<T>> implements Transcoder<T> {
  private static final byte[] EMPTY_BYTES = new byte[0];

  @Override
  public T fromBytes(@Nullable byte[] data) throws SingularityTranscoderException {
    return fromString(data == null ? null : new String(data, UTF_8));
  }

  protected abstract T fromString(@Nullable String string);

  @Override
  public byte[] toBytes(@Nullable T object) throws SingularityTranscoderException {
    return object == null ? EMPTY_BYTES : object.name().getBytes(UTF_8);
  }
}
