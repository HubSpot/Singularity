package com.hubspot.singularity.data.transcoders;

import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;

public interface Transcoder<T> {

  public T transcode(byte[] data) throws SingularityJsonException;
  
  public byte[] toBytes(T object) throws SingularityJsonException;
  
}
