package com.hubspot.singularity.data.transcoders;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.annotation.Nullable;

public class StringTranscoder implements Transcoder<String> {

  public static final StringTranscoder INSTANCE = new StringTranscoder();

  private static final byte[] EMPTY_BYTES = new byte[0];

  @Override
  public String fromBytes(@Nullable byte[] data) {
    return data == null ? "" : new String(data, UTF_8);
  }

  @Override
  public byte[] toBytes(@Nullable String object) {
    return object == null ? EMPTY_BYTES : object.getBytes(UTF_8);
  }
}
