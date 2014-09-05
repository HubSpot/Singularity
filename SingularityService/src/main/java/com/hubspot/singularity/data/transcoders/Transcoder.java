package com.hubspot.singularity.data.transcoders;

import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;

public interface Transcoder<T> {

  T transcode(byte[] data) throws SingularityJsonException;

  byte[] toBytes(T object) throws SingularityJsonException;

}
