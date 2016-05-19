package com.hubspot.singularity.runner.base.shared;

public class ExceptionChainParser {
  public static boolean exceptionChainContains(Exception e, Class clazz) {
    Throwable cause = e;
    while (null != (cause = cause.getCause())) {
      if (clazz.isInstance(cause)) {
        return true;
      }
    }
    return false;
  }
}
