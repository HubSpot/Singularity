package com.hubspot.mesos;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.google.common.base.Throwables;

public class JavaUtils {

  public static final String LOGBACK_LOGGING_PATTERN = "%-5level [%d] [%.15thread] %logger{35} - %msg%n";
  
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

  public static boolean isHttpSuccess(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

  public static String getHostAddress() throws Exception {
    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    while (interfaces.hasMoreElements()) {
      NetworkInterface current = interfaces.nextElement();
      if (!current.isUp() || current.isLoopback() || current.isVirtual())
        continue;
      Enumeration<InetAddress> addresses = current.getInetAddresses();
      while (addresses.hasMoreElements()) {
        InetAddress current_addr = addresses.nextElement();
        if (current_addr.isLoopbackAddress())
          continue;
        if (current_addr instanceof Inet4Address) {
          return current_addr.getHostAddress();
        }
      }
    }
    throw new RuntimeException("Couldn't deduce host address");
  }

  public static String getHostName() {
    try {
      InetAddress addr = InetAddress.getLocalHost();

      String hostname = addr.getHostName();

      return hostname;
    } catch (Throwable t) {
      return null;
    }
  }

  private final static String DURATION_FORMAT = "mm:ss.S";

  public static String duration(final long start) {
    return DurationFormatUtils.formatDuration(System.currentTimeMillis() - start, DURATION_FORMAT);
  }

  public static String durationFromMillis(final long millis) {
    return DurationFormatUtils.formatDuration(millis, DURATION_FORMAT);
  }
  
  public static Thread awaitTerminationWithLatch(final CountDownLatch latch, final String threadNameSuffix, final ExecutorService service, final long millis) {
    Thread t = new Thread("ExecutorServiceTerminationWaiter-" + threadNameSuffix) {
      public void run() {
        try {
          service.awaitTermination(millis, TimeUnit.MILLISECONDS);
        } catch (Throwable t) {
        } finally {
          latch.countDown();
        }
      }
    };
    
    t.start();
    
    return t;
  }

}
