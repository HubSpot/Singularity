package com.hubspot.mesos;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
  public static String urlEncode(String string) {
    try {
      return URLEncoder.encode(string, CHARSET_UTF);
    } catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }

  public static String urlDecode(String string) {
    try {
      return URLDecoder.decode(string, CHARSET_UTF);
    } catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }
  
  public static String[] reverseSplit(String string, int numItems, String separator) {
    final String[] splits = string.split("\\" + separator);
    final String[] reverseSplit = new String[numItems];
    
    for (int i = 1; i < numItems; i++) {
      reverseSplit[numItems - i] = splits[splits.length - i];
    }
    
    final StringBuilder lastItemBldr = new StringBuilder();
    
    for (int s = 0; s < splits.length - numItems + 1; s++) {
      lastItemBldr.append(splits[s]);
      if (s < splits.length - numItems) {
        lastItemBldr.append(separator);
      }
    }
     
    reverseSplit[0] = lastItemBldr.toString();
    
    return reverseSplit;
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
