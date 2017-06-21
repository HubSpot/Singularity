package com.hubspot.singularity.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import javax.ws.rs.WebApplicationException;

import org.junit.Assert;
import org.junit.Before;
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
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.UIConfiguration;
import com.hubspot.singularity.data.history.DeployHistoryHelper;


public class ValidatorTest extends SingularityTestBaseNoDb {

  @Inject
  private SingularityConfiguration singularityConfiguration;
  @Inject
  private DeployHistoryHelper deployHistoryHelper;
  @Inject
  private PriorityManager priorityManager;
  @Inject
  private DisasterManager disasterManager;
  @Inject
  private SlaveManager slaveManager;
  @Inject
  private UIConfiguration uiConfiguration;

  private SingularityValidator validator;

  @Before
  public void createValidator() {
    validator = new SingularityValidator(singularityConfiguration, deployHistoryHelper, priorityManager, disasterManager, slaveManager, uiConfiguration);
  }

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

  @Test
  public void itForbidsHealthCheckGreaterThanMaxTotalHealthCheck() {
    singularityConfiguration.setHealthcheckMaxTotalTimeoutSeconds(Optional.of(100));
    createValidator();

    // Total wait time on this request is (startup time) + ((interval) + (http timeout)) * (1 + retries)
    // = 50 + (5 + 5) * (9 + 1)
    // = 150
    HealthcheckOptions healthCheck = new HealthcheckOptionsBuilder("/")
        .setPortNumber(Optional.of(8080L))
        .setStartupTimeoutSeconds(Optional.of(50))
        .setIntervalSeconds(Optional.of(5))
        .setResponseTimeoutSeconds(Optional.of(5))
        .setMaxRetries(Optional.of(9))
        .build();
    SingularityDeploy deploy = SingularityDeploy
        .newBuilder("1234567", "1234567")
        .setHealthcheck(Optional.of(healthCheck))
        .setCommand(Optional.of("sleep 100;"))
        .build();
    SingularityRequest request = new SingularityRequestBuilder("1234567", RequestType.SERVICE).build();

    WebApplicationException exn = (WebApplicationException) catchThrowable(() -> validator.checkDeploy(request, deploy));
    System.out.println(exn.getResponse().getEntity());
    assertThat((String) exn.getResponse().getEntity())
        .contains("Max healthcheck time");
  }

}
