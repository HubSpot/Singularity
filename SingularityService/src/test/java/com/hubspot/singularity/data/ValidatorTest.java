package com.hubspot.singularity.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.junit.Assert;
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
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTestBaseNoDb;
import com.hubspot.singularity.api.SingularityRunNowRequest;


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

    validator.checkDeploy(singularityRequest, singularityDeploy, Collections.emptyList(), Collections.emptyList());
  }

  @Test
  public void itForbidsQuotesInDeployIds() throws Exception {
    final String badDeployId = "deployKey'";

    SingularityDeploy singularityDeploy = SingularityDeploy.newBuilder(badDeployId, badDeployId).build();
    SingularityRequest singularityRequest = new SingularityRequestBuilder(badDeployId, RequestType.SERVICE).build();

    WebApplicationException exn = (WebApplicationException) catchThrowable(() -> validator.checkDeploy(singularityRequest, singularityDeploy, Collections.emptyList(), Collections.emptyList()));
    assertThat((String) exn.getResponse().getEntity())
        .contains("[a-zA-Z0-9_]");
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

  @Test
  public void whenRunNowSetAndScheduledTaskAndAlreadyRunningItWillNotRunImmediately() {
    String requestId = "request";
    String deployID = "deploy";
    SingularityRequest request = new SingularityRequestBuilder(requestId, RequestType.SCHEDULED)
        .build();
    Optional<SingularityRunNowRequest> runNowRequest = Optional.of(runNowRequest());
    SingularityTaskId activeTask = new SingularityTaskId(requestId, deployID, 0, 1, "host", "rack");

    SingularityDeploy deploy = SingularityDeploy.newBuilder(requestId, deployID)
        .setCommand(Optional.of("printenv"))
        .setRunImmediately(runNowRequest)
        .build();

    SingularityDeploy result = validator.checkDeploy(request, deploy, Collections.singletonList(activeTask()), Collections.emptyList());

    Assert.assertFalse("Run immediately is no longer set", result.getRunImmediately().isPresent());
  }

  @Test
  public void whenRunNowSetAndOneOffAndTasksAlreadyRunningItWillNotRunImmediately() {
    String requestId = "request";
    String deployID = "deploy";
    SingularityRequest request = new SingularityRequestBuilder(requestId, RequestType.ON_DEMAND)
        .setInstances(Optional.of(2))
        .build();
    Optional<SingularityRunNowRequest> runNowRequest = Optional.of(runNowRequest());
    SingularityTaskId activeTask1 = new SingularityTaskId(requestId, deployID, 0, 1, "host", "rack");
    SingularityTaskId activeTask2 = new SingularityTaskId(requestId, deployID, 0, 1, "host", "rack");

    SingularityDeploy deploy = SingularityDeploy.newBuilder(requestId, deployID)
        .setCommand(Optional.of("printenv"))
        .setRunImmediately(runNowRequest)
        .build();

    SingularityDeploy result = validator.checkDeploy(request, deploy, Arrays.asList(activeTask1, activeTask2), Collections.emptyList());

    Assert.assertFalse("Run immediately is no longer set", result.getRunImmediately().isPresent());
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
    return new SingularityRunNowRequest(
        Optional.of("message"),
        Optional.of(false),
        Optional.of(runId),
        Optional.of(Collections.singletonList("--help")),
        Optional.absent()
    );
  }

  private SingularityRunNowRequest runNowRequest() {
    return new SingularityRunNowRequest(
        Optional.of("message"),
        Optional.of(false),
        Optional.absent(),
        Optional.of(Collections.singletonList("--help")),
        Optional.absent()
    );
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

}
