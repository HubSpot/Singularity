package com.hubspot.mesos;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

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

  public static String getHostAddress() throws Exception {
    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    while (interfaces.hasMoreElements()){
      NetworkInterface current = interfaces.nextElement();
      if (!current.isUp() || current.isLoopback() || current.isVirtual()) continue;
      Enumeration<InetAddress> addresses = current.getInetAddresses();
      while (addresses.hasMoreElements()){
        InetAddress current_addr = addresses.nextElement();
        if (current_addr.isLoopbackAddress()) continue;
        if (current_addr instanceof Inet4Address) {
          return current_addr.getHostAddress();
        }
      }
    }
    throw new RuntimeException("Couldn't deduce host address");
  }
}
