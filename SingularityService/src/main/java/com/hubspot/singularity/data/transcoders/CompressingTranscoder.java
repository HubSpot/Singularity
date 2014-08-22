package com.hubspot.singularity.data.transcoders;

import org.iq80.snappy.Snappy;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;
import com.hubspot.singularity.config.SingularityConfiguration;

public abstract class CompressingTranscoder<T> implements Transcoder<T> {

  private final SingularityConfiguration configuration;

  @Inject
  public CompressingTranscoder(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  private byte[] getMaybeCompressedBytes(byte[] bytes) {
    if (configuration.isCompressLargeDataObjects()) {
      return Snappy.compress(bytes);
    }
    return bytes;
  }

  protected abstract T actualTranscode(byte[] data);
  protected abstract byte[] actualToBytes(T object);

  @Override
  public T transcode(byte[] data) throws SingularityJsonException {
    return actualTranscode(getMaybeUncompressedBytes(data));
  }

  @Override
  public byte[] toBytes(T object) throws SingularityJsonException {
    return getMaybeCompressedBytes(actualToBytes(object));
  }

  private byte[] getMaybeUncompressedBytes(byte[] bytes) {
    if (configuration.isCompressLargeDataObjects()) {
      return Snappy.uncompress(bytes, 0, bytes.length);
    }
    return bytes;
  }

}
