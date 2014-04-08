package com.hubspot.singularity;

import org.apache.commons.lang.time.DurationFormatUtils;

public class Utils {

  private final static String DURATION_FORMAT = "mm:ss.S";
  
  public static String duration(final long start) {
    return DurationFormatUtils.formatDuration(System.currentTimeMillis() - start, DURATION_FORMAT);
  }
  
  public static String durationFromMillis(final long millis) {
    return DurationFormatUtils.formatDuration(millis, DURATION_FORMAT);
  }
  
}
