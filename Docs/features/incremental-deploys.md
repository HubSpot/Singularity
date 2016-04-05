### Incremental Deploys

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

