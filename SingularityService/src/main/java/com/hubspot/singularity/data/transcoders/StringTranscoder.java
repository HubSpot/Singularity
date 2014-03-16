package com.hubspot.singularity.data.transcoders;

import com.hubspot.mesos.JavaUtils;

public class StringTranscoder implements Transcoder<String> {

  public final static StringTranscoder STRING_TRANSCODER = new StringTranscoder();
  
  @Override
  public String transcode(byte[] data) throws Exception {
    return JavaUtils.toString(data);
  }
  
}
