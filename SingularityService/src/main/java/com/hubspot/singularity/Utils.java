package com.hubspot.singularity;

import org.apache.commons.lang.time.DurationFormatUtils;

public class Utils {

  public static String duration(final long start) {
    return DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
  }
  
}
