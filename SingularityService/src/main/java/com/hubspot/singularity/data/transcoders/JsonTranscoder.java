package com.hubspot.singularity.data.transcoders;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonTranscoder<T> implements Transcoder<T> {
  private static final byte[] EMPTY_BYTES = new byte[0];

  private final ObjectMapper objectMapper;
  private final Class<T> clazz;

  public JsonTranscoder(final ObjectMapper objectMapper, final Class<T> clazz) {
    this.objectMapper = checkNotNull(objectMapper, "objectMapper is null");
    this.clazz = checkNotNull(clazz, "clazz is null");
  }

  @Override
  public T fromBytes(@Nullable byte[] data) throws SingularityTranscoderException {
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
  public byte[] toBytes(@Nullable T object) throws SingularityTranscoderException {
    try {
      return object == null ? EMPTY_BYTES : objectMapper.writeValueAsBytes(object);
    } catch (IOException e) {
      throw new SingularityTranscoderException(e);
    }
  }
}
