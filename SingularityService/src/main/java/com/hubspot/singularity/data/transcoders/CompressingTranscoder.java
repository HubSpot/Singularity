package com.hubspot.singularity.data.transcoders;

import org.iq80.snappy.Snappy;

import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;

public class CompressingTranscoder {

  private final SingularityConfiguration configuration;

  @Inject
  public CompressingTranscoder(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }
  
  protected byte[] getMaybeCompressedBytes(byte[] bytes) {
    if (configuration.isCompressLargeDataObjects()) {
      return Snappy.compress(bytes);
    }
    return bytes;
  }
  
  protected byte[] getMaybeUncompressedBytes(byte[] bytes) {
    if (configuration.isCompressLargeDataObjects()) {
      return Snappy.uncompress(bytes, 0, bytes.length);
    }
    return bytes;
  }
  
}
