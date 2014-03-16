package com.hubspot.singularity.data.transcoders;

public interface Transcoder<T> {

  public T transcode(byte[] data) throws Exception;
  
}
