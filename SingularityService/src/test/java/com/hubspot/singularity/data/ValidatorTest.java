package com.hubspot.singularity.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRunNowRequestBuilder;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTestBaseNoDb;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.UIConfiguration;
import com.hubspot.singularity.data.history.DeployHistoryHelper;
import com.hubspot.singularity.api.SingularityRunNowRequest;


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

    validator.checkDeploy(singularityRequest, singularityDeploy, Collections.emptyList(), Collections.emptyList());
  }

  @Test
  public void itForbidsQuotesInDeployIds() throws Exception {
    final String badDeployId = "deployKey'";

    SingularityDeploy singularityDeploy = SingularityDeploy.newBuilder(badDeployId, badDeployId).build();
    SingularityRequest singularityRequest = new SingularityRequestBuilder(badDeployId, RequestType.SERVICE).build();

    WebApplicationException exn = (WebApplicationException) catchThrowable(() -> validator.checkDeploy(singularityRequest, singularityDeploy, Collections.emptyList(), Collections.emptyList()));
    assertThat((String) exn.getResponse().getEntity())
        .contains("[a-zA-Z0-9_.]");
  }

  @Test(expected = WebApplicationException.class)
  public void itForbidsTooLongDeployId() {
    String requestId = "requestId";
    SingularityRequest request = new SingularityRequestBuilder(requestId, RequestType.SCHEDULED)
        .build();

    SingularityDeploy deploy = SingularityDeploy.newBuilder(requestId, tooLongId())
        .build();

    validator.checkDeploy(request, deploy, Collections.emptyList(), Collections.emptyList());
  }

  @Test(expected = WebApplicationException.class)
  public void itForbidsRunNowOfScheduledWhenAlreadyRunning() {
    String deployID = "deploy";
    Optional<String> userEmail = Optional.absent();
    SingularityRequest request = new SingularityRequestBuilder("request2", RequestType.SCHEDULED)
        .setInstances(Optional.of(1))
        .build();
    Optional<SingularityRunNowRequest> runNowRequest = Optional.absent();
    List<SingularityTaskId> activeTasks = Collections.singletonList(activeTask());
    List<SingularityPendingTaskId> pendingTasks = Collections.emptyList();

    validator.checkRunNowRequest(deployID, userEmail, request, runNowRequest, activeTasks, pendingTasks);
  }

  @Test(expected = WebApplicationException.class)
  public void whenRunNowItForbidsMoreInstancesForOnDemandThanInRequest() {
    String deployID = "deploy";
    Optional<String> userEmail = Optional.absent();
    SingularityRequest request = new SingularityRequestBuilder("request2", RequestType.ON_DEMAND)
        .setInstances(Optional.of(1))
        .build();
    Optional<SingularityRunNowRequest> runNowRequest = Optional.absent();
    List<SingularityTaskId> activeTasks = Collections.singletonList(activeTask());
    List<SingularityPendingTaskId> pendingTasks = Collections.emptyList();

    validator.checkRunNowRequest(deployID, userEmail, request, runNowRequest, activeTasks, pendingTasks);
  }

  @Test(expected = WebApplicationException.class)
  public void whenRunNowItForbidsRequestsForLongRunningTasks() {
    String deployID = "deploy";
    Optional<String> userEmail = Optional.absent();
    SingularityRequest request = new SingularityRequestBuilder("request2", RequestType.SERVICE)
        .build();
    Optional<SingularityRunNowRequest> runNowRequest = Optional.absent();
    List<SingularityTaskId> activeTasks = Collections.emptyList();
    List<SingularityPendingTaskId> pendingTasks = Collections.emptyList();

    validator.checkRunNowRequest(deployID, userEmail, request, runNowRequest, activeTasks, pendingTasks);
  }

  @Test(expected = WebApplicationException.class)
  public void whenRunNowItForbidsTooLongRunIds() {
    String deployID = "deploy";
    Optional<String> userEmail = Optional.absent();
    SingularityRequest request = new SingularityRequestBuilder("request2", RequestType.SERVICE)
        .build();
    Optional<SingularityRunNowRequest> runNowRequest = Optional.of(runNowRequest(tooLongId()));
    List<SingularityTaskId> activeTasks = Collections.emptyList();
    List<SingularityPendingTaskId> pendingTasks = Collections.emptyList();

    validator.checkRunNowRequest(deployID, userEmail, request, runNowRequest, activeTasks, pendingTasks);
  }

  @Test
  public void whenRunNowIfRunIdSetItWillBePropagated() {
    String deployID = "deploy";
    Optional<String> userEmail = Optional.absent();
    SingularityRequest request = new SingularityRequestBuilder("request2", RequestType.ON_DEMAND)
        .build();
    Optional<SingularityRunNowRequest> runNowRequest = Optional.of(runNowRequest("runId"));
    List<SingularityTaskId> activeTasks = Collections.emptyList();
    List<SingularityPendingTaskId> pendingTasks = Collections.emptyList();

    SingularityPendingRequest pendingRequest = validator.checkRunNowRequest(deployID, userEmail, request, runNowRequest, activeTasks, pendingTasks);

    Assert.assertEquals("runId", pendingRequest.getRunId().get());
  }

  @Test
  public void whenRunNowIfNoRunIdSetItWillGenerateAnId() {
    String deployID = "deploy";
    Optional<String> userEmail = Optional.absent();
    SingularityRequest request = new SingularityRequestBuilder("request2", RequestType.ON_DEMAND)
        .build();
    Optional<SingularityRunNowRequest> runNowRequest = Optional.of(runNowRequest());
    List<SingularityTaskId> activeTasks = Collections.emptyList();
    List<SingularityPendingTaskId> pendingTasks = Collections.emptyList();

    SingularityPendingRequest pendingRequest = validator.checkRunNowRequest(deployID, userEmail, request, runNowRequest, activeTasks, pendingTasks);

    Assert.assertTrue(pendingRequest.getRunId().isPresent());
  }

  @Test
  public void whenDeployHasRunNowSetAndNotDeployedItWillRunImmediately() {
    String requestId = "request";
    String deployID = "deploy";
    SingularityRequest request = new SingularityRequestBuilder(requestId, RequestType.ON_DEMAND)
        .build();
    Optional<SingularityRunNowRequest> runNowRequest = Optional.of(runNowRequest());

    SingularityDeploy deploy = SingularityDeploy.newBuilder(requestId, deployID)
        .setCommand(Optional.of("printenv"))
        .setRunImmediately(runNowRequest)
        .build();

    SingularityDeploy result = validator.checkDeploy(request, deploy, Collections.emptyList(), Collections.emptyList());
    Assert.assertTrue(result.getRunImmediately().isPresent());
    Assert.assertTrue(result.getRunImmediately().get().getRunId().isPresent());
  }

  @Test(expected = WebApplicationException.class)
  public void whenDeployHasRunNowSetItValidatesThatItIsLessThanACertaionLength() {
    String requestId = "request";
    String deployID = "deploy";
    SingularityRequest request = new SingularityRequestBuilder(requestId, RequestType.ON_DEMAND)
        .build();
    Optional<SingularityRunNowRequest> runNowRequest = Optional.of(runNowRequest(tooLongId()));

    SingularityDeploy deploy = SingularityDeploy.newBuilder(requestId, deployID)
        .setCommand(Optional.of("printenv"))
        .setRunImmediately(runNowRequest)
        .build();

    validator.checkDeploy(request, deploy, Collections.emptyList(), Collections.emptyList());
  }

  @Test(expected = WebApplicationException.class)
  public void whenDeployNotOneOffOrScheduledItForbidsRunImmediately() {
    String requestId = "request";
    String deployID = "deploy";
    SingularityRequest request = new SingularityRequestBuilder(requestId, RequestType.WORKER)
        .build();
    Optional<SingularityRunNowRequest> runNowRequest = Optional.of(runNowRequest());

    SingularityDeploy deploy = SingularityDeploy.newBuilder(requestId, deployID)
        .setCommand(Optional.of("printenv"))
        .setRunImmediately(runNowRequest)
        .build();

    validator.checkDeploy(request, deploy, Collections.emptyList(), Collections.emptyList());
  }

  private SingularityTaskId activeTask() {
    return new SingularityTaskId(
        "requestId",
        "deployId",
        System.currentTimeMillis(),
        1,
        "host",
        "rack"
    );
  }

  private SingularityRunNowRequest runNowRequest(String runId) {
    return new SingularityRunNowRequestBuilder()
        .setMessage("message")
        .setSkipHealthchecks(false)
        .setRunId(runId)
        .setCommandLineArgs(Collections.singletonList("--help"))
        .build();
  }

  private SingularityRunNowRequest runNowRequest() {
    return new SingularityRunNowRequestBuilder()
        .setMessage("message")
        .setSkipHealthchecks(false)
        .setCommandLineArgs(Collections.singletonList("--help"))
        .build();
  }

  private String tooLongId() {
    char[] runId = new char[150];
    Arrays.fill(runId, 'x');
    return new String(runId);
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

    WebApplicationException exn = (WebApplicationException) catchThrowable(() -> validator.checkDeploy(request, deploy, Collections.emptyList(), Collections.emptyList()));
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

    WebApplicationException exn = (WebApplicationException) catchThrowable(() -> validator.checkDeploy(request, deploy, Collections.emptyList(), Collections.emptyList()));
    System.out.println(exn.getResponse().getEntity());
    assertThat((String) exn.getResponse().getEntity())
        .contains("Max healthcheck time");
  }

  @Test
  public void itAllowsWorkerToServiceTransitionIfNotLoadBalanced() {
    SingularityRequest request = new SingularityRequestBuilder("test", RequestType.WORKER)
        .build();
    SingularityRequest newRequest = new SingularityRequestBuilder("test", RequestType.SERVICE)
        .build();
    SingularityRequest result = validator.checkSingularityRequest(newRequest, Optional.of(request), Optional.absent(), Optional.absent());
    Assert.assertEquals(newRequest.getRequestType(), result.getRequestType());
  }

  @Test(expected = WebApplicationException.class)
  public void itDoesNotWorkerToServiceTransitionIfLoadBalanced() {
    SingularityRequest request = new SingularityRequestBuilder("test", RequestType.WORKER)
        .build();
    SingularityRequest newRequest = new SingularityRequestBuilder("test", RequestType.SERVICE)
        .setLoadBalanced(Optional.of(true))
        .build();
    validator.checkSingularityRequest(newRequest, Optional.of(request), Optional.absent(), Optional.absent());
  }

  @Test(expected = WebApplicationException.class)
  public void itDoesNotAllowOtherRequestTypesToChange() {
    SingularityRequest request = new SingularityRequestBuilder("test", RequestType.ON_DEMAND)
        .build();
    SingularityRequest newRequest = new SingularityRequestBuilder("test", RequestType.SCHEDULED)
        .build();
    validator.checkSingularityRequest(newRequest, Optional.of(request), Optional.absent(), Optional.absent());
  }
}
