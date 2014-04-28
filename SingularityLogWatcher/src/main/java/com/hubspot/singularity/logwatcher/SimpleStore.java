package com.hubspot.singularity.logwatcher;

import java.io.Closeable;
import java.util.List;

import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.config.TailMetadata;

public interface SimpleStore extends Closeable {

  @SuppressWarnings("serial")
  public static class StoreException extends RuntimeException {

    public StoreException(String message, Throwable cause) {
      super(message, cause);
    }

    public StoreException(String message) {
      super(message);
    }

    public StoreException(Throwable cause) {
      super(cause);
    }

    public StoreException() {
      super();
    }
    
  }
  
  public void start(); 
  
  public void markConsumed(TailMetadata tail) throws StoreException;
  
  public void savePosition(TailMetadata tail, long position) throws StoreException;
 
  public Optional<Long> getPosition(TailMetadata tail) throws StoreException;

  public List<TailMetadata> getTails();
  
  public void registerListener(TailMetadataListener listener);
  
  public void removeListener(TailMetadataListener listener);
  
}
