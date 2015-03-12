package com.hubspot.singularity.data.transcoders;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.singularity.config.SingularityConfiguration;

public class CompressingJsonTranscoder<T> extends CompressingTranscoder<T> {
  private static final byte[] EMPTY_BYTES = new byte[0];

  private final ObjectMapper objectMapper;
  private final Class<T> clazz;

  CompressingJsonTranscoder(final SingularityConfiguration configuration, final ObjectMapper objectMapper, final Class<T> clazz) {
    super(configuration);
    this.objectMapper = checkNotNull(objectMapper, "objectMapper is null");
    this.clazz = checkNotNull(clazz, "clazz is null");
  }

  @Override
  protected T actualFromBytes(@Nullable byte[] data) throws SingularityTranscoderException {
    if (data == null || data.length == 0) {
      return null;
    }

    try {
      return objectMapper.readValue(data, clazz);
    } catch (IOException e) {
      throw new SingularityTranscoderException(e);
    }
  }

  @Override
  protected byte[] actualToBytes(@Nullable T object) throws SingularityTranscoderException {
    try {
      return object == null ? EMPTY_BYTES : objectMapper.writeValueAsBytes(object);
    } catch (IOException e) {
      throw new SingularityTranscoderException(e);
    }
  }
}
