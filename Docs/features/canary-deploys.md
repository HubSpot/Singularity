## Canary Deploys

As of `1.5.0`, a new implementation of canary deploys replaces the old incremental deploys in Singularity. Some initial notes on the update:

- Previous behavior will be preserved if using `incrementalDeploys` and not specifying a `canarySettings` object in your SingularityDeploy json
- Default behavior remains unchanged for deploys, settings can be explicitly enabled per deploy

### Starting a Canary Deploy

To run a canary deploy, simply specify a `canarySettings` object in your SingularityDeploy json (defaults with canary disabled are shown  in the json below):

```
{
  "deploy": {
    "canarySettings": {
      "enableCanaryDeploy":false,
      "instanceGroupSize":1,
      "acceptanceMode":"NONE",
      "waitMillisBetweenGroups":0,
      "allowedTasksFailuresPerGroup":0,
      "canaryCycleCount":3
    },
    ...
  }
}
```

- `acceptanceMode` - defaults to `NONE`
  - `NONE` - No additional checks are run against a deploy
  - `TIMED` - Wait a set amount of time between deploy steps. Relevant if `enableCanaryDeploy` is `true`
  - `CHECKS` - Run all bound implementations of `DeployAcceptanceHook` (see more info below) after each deploy step. Applies to all tasks at once if `enableCanaryDeploy` is `false` and will run on each individual canary step if `enableCanaryDeploy` is `true`
- `enableCanaryDeploy` - Defaults to `false`. If `true` enables a step-wise deploy and use of the other canary settings fields (see below for more details). If `false`, performs a normal atomic deploy where all new instances are spun up and all old ones taken down ones new are healthy.
  - _Load balancer note_: If `false` will add all new and remove all old instances in the LB during a deploy in one atomic operation. If `true` new instances will be added to the load balancer alongside old ones, and old ones cleaned after the deploy has fully succeeded.
- `instanceGroupSize` - The number of instances to start per canary group. e.g. if set to `1`, the canary deploy will start `1` instance -> health/acceptance check -> spin down `1` old instance -> start `1` new -> etc
- `waitMillisBetweenGroups` - If `acceptanceMode` is set to `TIMED`, wait this long between groups of new instances of size `instanceGroupSize` (e.g. launch `1`, wait 10 minutes, launch `1` and so on)
- `canaryCycleCount` - Run this many rounds of canary steps before skipping to the full request scale. e.g. if deploying a request of scale `10` and `canaryCycleCount` is set to `3`, 3 instances will be launched one at a time, then the remaining 7 will be launched all at once in a final step
- `allowedTasksFailuresPerGroup` - Replaces the global configuration for allowed task failures in a deploy. For each canary deploy step, this many tasks are allowed to fail and retry before the deploy is considered to have failed

### Custom Deploy Hooks

Applies when `acceptanceMode` is set to `CHECKS`. For those extending SingularityService, you can bind any additional number of implementations of `DeployAcceptanceHook` in guice modules like:

```
  Multibinder
      .newSetBinder(binder, DeployAcceptanceHook.class)
      .addBinding()
      .to(MyAcceptanceHook.class);
```

Each implementation of an acceptance hook should look like:

```java
public class MyAcceptanceHook implements DeployAcceptanceHook {

  @Inject
  public MyAcceptanceHook() {}

  @Override
  public boolean isFailOnUncaughtException() {
    // If `true` an uncaught exception fails a deploy,
    // if `false` the deploy can still succeed. Useful for testing
    return false;
  }

  @Override
  public String getName() {
    // Should be unique per hook
    return "My-Test-Hook";
  }

  @Override
  public DeployAcceptanceResult getAcceptanceResult(
    SingularityRequest request, // request object. Reflects any updates made during deploy
    SingularityDeploy deploy, // Full deploy json object
    SingularityPendingDeploy pendingDeploy, // Pending deploy state
    Collection<SingularityTaskId> activeTasksForPendingDeploy, // Tasks that are part of the current pending deploy
    Collection<SingularityTaskId> inactiveTasksForPendingDeploy, // Tasks from the pending deploy which may have shut down or crashed
    Collection<SingularityTaskId> otherActiveTasksForRequest // Tasks from other deploys (e.g. the previous active one)
  ) {
    // Do stuff here
    return new DeployAcceptanceResult(
      DeployAcceptanceState.SUCCEEDED,
      "Test hook passed"
    );
  }
}
```

The `canarySettings` object will change the state/time during which `getAcceptanceResult` is called:
- If `enableCanaryDeploy` is set to `false`, the state of tasks will be:
  - All new tasks in `activeTasksForPendingDeploy` are launched and health checked
  - If the deploy is load balanced, tasks in `otherActiveTasksForRequest` are no longer in the load balancer. Only the new deploy tasks in `activeTasksForPendingDeploy` are active in the load balancer
    - *Note* - Singularity will re-add the old tasks back to the load balancer if deploy acceptance checks fail
- If `enableCanaryDeploy` is set to `true`, `getAcceptanceResult` is called after each deploy step
  - `activeTasksForPendingDeploy` contains _all_ active tasks launched so far, not just those for the current canary step. These tasks are in running state and have passed initial health checks
  - If load balanced, all tasks in `activeTasksForPendingDeploy` as well as all in `otherActiveTasksForRequest` are active in the load balancer at once

#### Available Data For Hooks

Since hooks are compiled into the Singularity jar and exstentions of SingularityService, all classes available in guice are also available to the hook. In particular:
- `TaskManager` - On the leader most calls here will be in memory lookups and can be used to fetch the full data for a task (ports, environment, etc)
- `AsyncHttpClient` - Singularity's default http client
- `@Singularity ObjectMapper` - pre-configured object mapper for Singularity objects

### Incremental Deploys (Deprecated)

_Deprecated_: behavior will be preserved, but prefer using the newer `canarySettings` documented above. Incremental deploys are essentially equivalent to using an `acceptanceMode` of `TIMED`.

As of `0.5.0` Singularity supports an incremental deploy for finer-grained control when rolling out new changes. This deploy is enabled via a few extra fields on the `SingularityDeploy` object when starting a deploy:

- `deployInstanceCountPerStep`: Deploy this many instances at a time until the total instance count for the request is reached is reached (`Optional<Integer>`, default is all instances at once)
- `deployStepWaitTimeMs`: Wait this many milliseconds between deploy steps before continuing to deploy the next `deployInstanceCountPerStep` instances (`Optional<Integer>`, default is 0, i.e. continue immediately)
- `autoAdvanceDeploySteps`: automatically advance to the next target instance count after `deployStepWaitTimeMs` seconds (`Optional<Boolean>`, defaults to `true`). If this is `false`, then manual confirmation will be needed to move to the next target instance count. This can be done via the ui.


#### Example

`TestService` is currently running `3` instances. During the next deploy, you want to replace only `1` of these instances at a time and have Singularity wait at least a minute after deploying one so you can verify that everything works as expected. The following fields can be added to the deploy json to accomplish this:

```
deployInstanceCountPerStep: 1
deployStepWaitTimeMs: 60000
autoAdvanceDeploySteps: true
```

When the deploy starts, Singularity will start `1` (`deployInstanceCountPerStep`) instance from the new deploy (The `3` old instances will still be running). Once the new task is determined to be healthy a few things happen:

- Singularity will add the instance from the new deploy to the load balancer (if applicable)
- Singularity will shut down `1` (`deployInstanceCountPerStep`) of the instances from the old deploy after removing it from the load balancer (if applicable)
- Singularity will start counting down the `60000 ms` until it launches the next `deployInstanceCountPerStep` instances

Once the `deployStepWaitTimeMs` of wait time has elapsed, Singularity will start this process again, launching a second task for the new deploy, waiting until it is healthy, then shutting down a task from the old deploy. This will continue until the deploy fails, the deploy is cancelled, or all instances are part of the new deploy and it succeeds.

A few more things to note about the incremental deploy process:
- If the deploy fails or is cancelled, Singularity replaces any missing instances from the old deploy and makes sure they are healthy before shutting down active/healthy instances from the new deploy. (i.e. you will never be under capacity)
- At any time, it is possible to advance the deploy to another target instance count via the UI or API. In other words, you can skip the remaining `deployStepWaitTimeMs`, skip steps of the deploy, or even decrease the instance count to roll back a step.

