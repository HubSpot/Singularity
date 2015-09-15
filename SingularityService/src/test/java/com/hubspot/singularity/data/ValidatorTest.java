package com.hubspot.singularity.data;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTestBaseNoDb;


public class ValidatorTest extends SingularityTestBaseNoDb {

  @Inject
  private SingularityValidator validator;

  @Test
  public void testCronExpressionHandlesDayIndexing() {
    Assert.assertEquals("0 0 12 ? * 1", validator.getQuartzScheduleFromCronSchedule("0 12 * * 0"));
    Assert.assertEquals("0 0 12 ? * 1-6", validator.getQuartzScheduleFromCronSchedule("0 12 * * 0-5"));
    Assert.assertEquals("0 0 12 ? * 1,2,3,4", validator.getQuartzScheduleFromCronSchedule("0 12 * * 0,1,2,3"));
    Assert.assertEquals("0 0 12 ? * MON,TUES,WED", validator.getQuartzScheduleFromCronSchedule("0 12 * * MON,TUES,WED"));
    Assert.assertEquals("0 0 12 ? * MON-WED", validator.getQuartzScheduleFromCronSchedule("0 12 * * MON-WED"));
  }

}
