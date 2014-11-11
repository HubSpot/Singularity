package com.hubspot.singularity;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;

public class SingularityS3Test {

  @Test
  public void testS3FormatHelper() throws Exception {
    SingularityTaskId taskId = new SingularityTaskId("rid", "did", 1, 1, "host", "rack");

    long start = 1414610537117l; // Wed, 29 Oct 2014 19:22:17 GMT
    long end = 1415724215000l; // Tue, 11 Nov 2014 16:43:35 GMT

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes("%Y/%m/%taskId", taskId, Optional.<String> absent(), start, end);

    Assert.assertTrue(prefixes.size() == 2);

    end = 1447265861000l; // Tue, 11 Nov 2015 16:43:35 GMT

    prefixes = SingularityS3FormatHelper.getS3KeyPrefixes("%Y/%taskId", taskId, Optional.<String> absent(), start, end);

    Assert.assertTrue(prefixes.size() == 2);

    start = 1415750399999l;
    end = 1415771999000l;

    prefixes = SingularityS3FormatHelper.getS3KeyPrefixes("%Y/%m/%d/%taskId", taskId, Optional.<String> absent(), start, end);

    System.out.println(prefixes);
    Assert.assertTrue(prefixes.size() == 2);



  }

}
