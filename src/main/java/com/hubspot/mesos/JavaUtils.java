package com.hubspot.mesos;

import java.io.UnsupportedEncodingException;

import com.google.common.base.Throwables;

public class JavaUtils {

  private static final String CHARSET_UTF = "UTF-8";
  
  public static byte[] toBytes(String string) {
    try {
      return string.getBytes(CHARSET_UTF);
    } catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }
  
  public static String toString(byte[] bytes) {
    try {
      return new String(bytes, CHARSET_UTF);
    } catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }
  
}
