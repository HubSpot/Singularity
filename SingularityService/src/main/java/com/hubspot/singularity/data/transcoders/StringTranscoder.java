package com.hubspot.singularity.data.transcoders;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StringTranscoder implements Transcoder<String> {

  public static final StringTranscoder STRING_TRANSCODER = new StringTranscoder();

  @Override
  public String transcode(byte[] data) {
    return new String(data, UTF_8);
  }

  @Override
  public byte[] toBytes(String object) {
    return object.getBytes(UTF_8);
  }

}
