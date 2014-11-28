package com.hubspot.singularity.data.transcoders;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import org.iq80.snappy.CorruptionException;
import org.iq80.snappy.Snappy;

import com.hubspot.singularity.config.SingularityConfiguration;

public abstract class CompressingTranscoder<T> implements Transcoder<T> {

  private final boolean compressLargeDataObjects;

  protected CompressingTranscoder(SingularityConfiguration configuration) {
    checkNotNull(configuration, "configuration is null");
    this.compressLargeDataObjects = configuration.isCompressLargeDataObjects();
  }

  protected abstract T actualFromBytes(byte[] data) throws SingularityTranscoderException;

  protected abstract byte[] actualToBytes(T object) throws SingularityTranscoderException;

  @Override
  public final T fromBytes(@Nullable byte[] data) throws SingularityTranscoderException {
    return actualFromBytes(getMaybeUncompressedBytes(data));
  }

  @Override
  public final byte[] toBytes(@Nullable T object) throws SingularityTranscoderException {
    return getMaybeCompressedBytes(actualToBytes(object));
  }

  private byte[] getMaybeCompressedBytes(@Nullable byte[] bytes) throws SingularityTranscoderException {

    if (bytes == null || bytes.length == 0) {
      return bytes;
    }

    return compressLargeDataObjects ? Snappy.compress(bytes) : bytes;
  }

  private byte[] getMaybeUncompressedBytes(@Nullable byte[] bytes) throws SingularityTranscoderException {

    if (bytes == null || bytes.length == 0) {
      return bytes;
    }

    try {
      return compressLargeDataObjects ? Snappy.uncompress(bytes, 0, bytes.length) : bytes;
    } catch (CorruptionException ce) {
      throw new SingularityTranscoderException(ce);
    }
  }
}
