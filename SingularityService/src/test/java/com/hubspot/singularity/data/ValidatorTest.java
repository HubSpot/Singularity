package com.hubspot.singularity.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityMesosTaskLabel;
import com.hubspot.singularity.HealthcheckProtocol;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.ScheduleType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityEmailDestination;
import com.hubspot.singularity.SingularityEmailType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTestBaseNoDb;
import com.hubspot.singularity.SlavePlacement;


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

  @Test(expected = WebApplicationException.class)
  public void itForbidsBracketCharactersInDeployIds() throws Exception {
    final String badDeployId = "deployKey[[";

    SingularityDeploy singularityDeploy = new SingularityDeploy(badDeployId, badDeployId,
        Optional.<String>absent(), Optional.<List<String>>absent(),
        Optional.<SingularityContainerInfo>absent(), Optional.<String>absent(),
        Optional.<String>absent(), Optional.<String>absent(),
        Optional.<Resources>absent(), Optional.<Resources>absent(),
        Optional.<Map<String,String>>absent(), Optional.<Map<Integer,Map<String,String>>>absent(),
        Optional.<List<String>>absent(), Optional.<Map<String,String>>absent(),
        Optional.<ExecutorData>absent(), Optional.<String>absent(),
        Optional.<Long>absent(), Optional.<Map<String,String>>absent(),
        Optional.<List<SingularityMesosTaskLabel>>absent(), Optional.<Map<Integer,Map<String,String>>>absent(),
        Optional.<Map<Integer,List<SingularityMesosTaskLabel>>>absent(), Optional.<Long>absent(),
        Optional.<String>absent(), Optional.<Long>absent(),
        Optional.<Long>absent(), Optional.<Integer>absent(),
        Optional.<Integer>absent(), Optional.<Long>absent(), Optional.<HealthcheckOptions>absent(),
        Optional.<String>absent(), Optional.<Set<String>>absent(),
        Optional.<Integer>absent(), Optional.<Long>absent(),
        Optional.<Map<String,Object>>absent(), Optional.<Set<String>>absent(),
        Optional.<List<String>>absent(), Optional.<String>absent(),
        Optional.<String>absent(), Optional.<String>absent(),
        Optional.<Boolean>absent(), Optional.<HealthcheckProtocol>absent(),
        Optional.<Integer>absent(), Optional.<Integer>absent(),
        Optional.<Boolean>absent(), Optional.<Integer>absent(),
        Optional.<Boolean>absent(), Optional.<String>absent());
    SingularityRequest singularityRequest = new SingularityRequest(badDeployId, RequestType.SERVICE,
        Optional.<List<String>>absent(), Optional.<Integer>absent(),
        Optional.<String>absent(), Optional.<Integer>absent(),
        Optional.<Boolean>absent(), Optional.<Boolean>absent(),
        Optional.<Long>absent(), Optional.<Long>absent(),
        Optional.<ScheduleType>absent(), Optional.<String>absent(),
        Optional.<String>absent(), Optional.<List<String>>absent(),
        Optional.<SlavePlacement>absent(), Optional.<Map<String,String>>absent(),
        Optional.<Map<String,String>>absent(), Optional.<Long>absent(),
        Optional.<Long>absent(), Optional.<String>absent(),
        Optional.<Set<String>>absent(), Optional.<Set<String>>absent(),
        Optional.<Boolean>absent(), Optional.<Boolean>absent(),
        Optional.<Map<SingularityEmailType,List<SingularityEmailDestination>>>absent(),
        Optional.<Boolean>absent(), Optional.<Boolean>absent(),
        Optional.<String>absent(), Optional.<Boolean>absent(),
        Optional.<Double>absent(), Optional.<Integer>absent(),
        Optional.<Boolean>absent(), Optional.<String>absent());

    validator.checkDeploy(singularityRequest, singularityDeploy);
  }

  @Test(expected = WebApplicationException.class)
  public void itForbidsQuotesInDeployIds() throws Exception {
    final String badDeployId = "deployKey'";

    SingularityDeploy singularityDeploy = new SingularityDeploy(badDeployId, badDeployId,
        Optional.<String>absent(), Optional.<List<String>>absent(),
        Optional.<SingularityContainerInfo>absent(), Optional.<String>absent(),
        Optional.<String>absent(), Optional.<String>absent(),
        Optional.<Resources>absent(), Optional.<Resources>absent(),
        Optional.<Map<String,String>>absent(), Optional.<Map<Integer,Map<String,String>>>absent(),
        Optional.<List<String>>absent(), Optional.<Map<String,String>>absent(),
        Optional.<ExecutorData>absent(), Optional.<String>absent(),
        Optional.<Long>absent(), Optional.<Map<String,String>>absent(),
        Optional.<List<SingularityMesosTaskLabel>>absent(), Optional.<Map<Integer,Map<String,String>>>absent(),
        Optional.<Map<Integer,List<SingularityMesosTaskLabel>>>absent(), Optional.<Long>absent(),
        Optional.<String>absent(), Optional.<Long>absent(),
        Optional.<Long>absent(), Optional.<Integer>absent(),
        Optional.<Integer>absent(), Optional.<Long>absent(), Optional.<HealthcheckOptions>absent(),
        Optional.<String>absent(), Optional.<Set<String>>absent(),
        Optional.<Integer>absent(), Optional.<Long>absent(),
        Optional.<Map<String,Object>>absent(), Optional.<Set<String>>absent(),
        Optional.<List<String>>absent(), Optional.<String>absent(),
        Optional.<String>absent(), Optional.<String>absent(),
        Optional.<Boolean>absent(), Optional.<HealthcheckProtocol>absent(),
        Optional.<Integer>absent(), Optional.<Integer>absent(),
        Optional.<Boolean>absent(), Optional.<Integer>absent(),
        Optional.<Boolean>absent(), Optional.<String>absent());
    SingularityRequest singularityRequest = new SingularityRequest(badDeployId, RequestType.SERVICE,
        Optional.<List<String>>absent(), Optional.<Integer>absent(),
        Optional.<String>absent(), Optional.<Integer>absent(),
        Optional.<Boolean>absent(), Optional.<Boolean>absent(),
        Optional.<Long>absent(), Optional.<Long>absent(),
        Optional.<ScheduleType>absent(), Optional.<String>absent(),
        Optional.<String>absent(), Optional.<List<String>>absent(),
        Optional.<SlavePlacement>absent(), Optional.<Map<String,String>>absent(),
        Optional.<Map<String,String>>absent(), Optional.<Long>absent(),
        Optional.<Long>absent(), Optional.<String>absent(),
        Optional.<Set<String>>absent(), Optional.<Set<String>>absent(),
        Optional.<Boolean>absent(), Optional.<Boolean>absent(),
        Optional.<Map<SingularityEmailType,List<SingularityEmailDestination>>>absent(),
        Optional.<Boolean>absent(), Optional.<Boolean>absent(),
        Optional.<String>absent(), Optional.<Boolean>absent(),
        Optional.<Double>absent(), Optional.<Integer>absent(),
        Optional.<Boolean>absent(), Optional.<String>absent());

    validator.checkDeploy(singularityRequest, singularityDeploy);
  }

}
