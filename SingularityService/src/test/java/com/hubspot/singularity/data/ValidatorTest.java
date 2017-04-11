package com.hubspot.singularity.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import javax.ws.rs.WebApplicationException;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.deploy.HealthcheckOptionsBuilder;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
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

  @Test(expected = WebApplicationException.class)
  public void itForbidsBracketCharactersInDeployIds() throws Exception {
    final String badDeployId = "deployKey[[";

    SingularityDeploy singularityDeploy = SingularityDeploy.newBuilder(badDeployId, badDeployId).build();
    SingularityRequest singularityRequest = new SingularityRequestBuilder(badDeployId, RequestType.SERVICE).build();

    validator.checkDeploy(singularityRequest, singularityDeploy);
  }

  @Test
  public void itForbidsQuotesInDeployIds() throws Exception {
    final String badDeployId = "deployKey'";

    SingularityDeploy singularityDeploy = SingularityDeploy.newBuilder(badDeployId, badDeployId).build();
    SingularityRequest singularityRequest = new SingularityRequestBuilder(badDeployId, RequestType.SERVICE).build();

    WebApplicationException exn = (WebApplicationException) catchThrowable(() -> validator.checkDeploy(singularityRequest, singularityDeploy));
    assertThat((String) exn.getResponse().getEntity())
        .contains("[a-zA-Z0-9_]");
  }

  @Test
  public void itForbidsHealthCheckStartupDelaysLongerThanKillWait() {
    // Default kill wait time is 10 minutes (600 seconds)
    HealthcheckOptions healthCheck = new HealthcheckOptionsBuilder("/")
        .setPortNumber(Optional.of(8080L))
        .setStartupDelaySeconds(Optional.of(10000))
        .build();
    SingularityDeploy deploy = SingularityDeploy
        .newBuilder("1234567", "1234567")
        .setHealthcheck(Optional.of(healthCheck))
        .build();
    SingularityRequest request = new SingularityRequestBuilder("1234567", RequestType.SERVICE).build();

    WebApplicationException exn = (WebApplicationException) catchThrowable(() -> validator.checkDeploy(request, deploy));
    assertThat((String) exn.getResponse().getEntity())
        .contains("Health check startup delay");
  }

}
