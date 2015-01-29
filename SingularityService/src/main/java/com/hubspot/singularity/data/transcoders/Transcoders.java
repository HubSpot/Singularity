package com.hubspot.singularity.data.transcoders;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Throwables;

public final class Transcoders {
  private Transcoders() {
    throw new AssertionError("do not instantiate");
  }

  public static <T> Function<T, byte[]> getToBytesFunction(final Transcoder<T> transcoder) {
    checkNotNull(transcoder, "transcoder is null");

    return new Function<T, byte[]>() {
      @Override
      public byte[] apply(@Nullable T value) {
        try {
          return transcoder.toBytes(value);
        } catch (Throwable e) {
          throw Throwables.propagate(e);
        }
      }
    };
  }

  public static <T> Function<byte[], T> getFromBytesFunction(final Transcoder<T> transcoder) {
    checkNotNull(transcoder, "transcoder is null");

    return new Function<byte[], T>() {
      @Override
      public T apply(@Nullable byte[] value) {
        try {
          return transcoder.fromBytes(value);
        } catch (Throwable e) {
          throw Throwables.propagate(e);
        }
      }
    };
  }

  public static <T> Function<String, T> getFromStringFunction(final Transcoder<T> transcoder) {
    checkNotNull(transcoder, "transcoder is null");

    return new Function<String, T>() {
      @Override
      public T apply(@Nullable String value) {
        if (value == null) {
          return null;
        }
        try {
          return transcoder.fromBytes(value.getBytes(UTF_8));
        } catch (Throwable e) {
          throw Throwables.propagate(e);
        }
      }
    };
  }
}
