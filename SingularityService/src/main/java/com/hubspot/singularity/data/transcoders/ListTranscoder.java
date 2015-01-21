package com.hubspot.singularity.data.transcoders;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ListTranscoder<T> implements Transcoder<List<T>> {
  private static final byte[] EMPTY_BYTES = new byte[0];

  private final ObjectMapper objectMapper;

  ListTranscoder(final ObjectMapper objectMapper) {
    this.objectMapper = checkNotNull(objectMapper, "objectMapper is null");
  }

  @Override
  public List<T> fromBytes(@Nullable byte[] data) throws SingularityTranscoderException {
    if (data == null || data.length == 0) {
      return null;
    }

    try {
      return objectMapper.readValue(data, new TypeReference<List<T>>() {});
    } catch (IOException e) {
      throw new SingularityTranscoderException(e);
    }
  }

  @Override
  public byte[] toBytes(@Nullable List<T> object) throws SingularityTranscoderException {
    try {
      return object == null ? EMPTY_BYTES : objectMapper.writeValueAsBytes(object);
    } catch (IOException e) {
      throw new SingularityTranscoderException(e);
    }
  }
}
