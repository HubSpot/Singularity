package com.hubspot.singularity.data;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTestBaseNoDb;


public class ValidatorTest extends SingularityTestBaseNoDb {

  @Inject
  private SingularityValidator validator;

  /**
   * Standard cron: day of week (0 - 6) (0 to 6 are Sunday to Saturday, or use names; 7 is Sunday, the same as 0)
   * Quartz: 1-7 or SUN-SAT
   */

  @Test
  public void testCronExpressionHandlesDayIndexing() {
    Assert.assertEquals("0 0 12 ? * SUN", validator.getQuartzScheduleFromCronSchedule("0 12 * * 7"));
    Assert.assertEquals("0 0 12 ? * SAT", validator.getQuartzScheduleFromCronSchedule("0 12 * * 6"));
    Assert.assertEquals("0 0 12 ? * SUN", validator.getQuartzScheduleFromCronSchedule("0 12 * * 0"));
    Assert.assertEquals("0 0 12 ? * SUN-FRI", validator.getQuartzScheduleFromCronSchedule("0 12 * * 0-5"));
    Assert.assertEquals("0 0 12 ? * SUN,MON,TUE,WED", validator.getQuartzScheduleFromCronSchedule("0 12 * * 0,1,2,3"));
    Assert.assertEquals("0 0 12 ? * MON,TUE,WED", validator.getQuartzScheduleFromCronSchedule("0 12 * * MON,TUE,WED"));
    Assert.assertEquals("0 0 12 ? * MON-WED", validator.getQuartzScheduleFromCronSchedule("0 12 * * MON-WED"));
  }

}
