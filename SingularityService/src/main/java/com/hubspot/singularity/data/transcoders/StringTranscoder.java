package com.hubspot.singularity.data.transcoders;

import com.hubspot.mesos.JavaUtils;

public class StringTranscoder implements Transcoder<String> {

  public static final StringTranscoder STRING_TRANSCODER = new StringTranscoder();

  @Override
  public String transcode(byte[] data) {
    return JavaUtils.toString(data);
  }

  @Override
  public byte[] toBytes(String object) {
    return JavaUtils.toBytes(object);
  }

}
