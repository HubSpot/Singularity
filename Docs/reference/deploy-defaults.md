## Deploy Defaults

The majority of fields on the [`SingularityDeployRequest`](apidocs/models.md#model-SingularityDeployRequest) and [`SingularityDeploy`](apidocs/models.md#model-SingularityDeploy) objects are optional. However, many of these values have request level or system-wide defaults to fall back on when not set. The list below contains a list of deploy properties, relevant `SingularityRequest` or `SingularityConfiguration` fields, and their defaults. Config fields are in the `SingularityConfiguration` unless otherwise specified

#### SingularityDeployRequest

| deploy field | config field | default value |
|--------------|--------------|---------------|
| `unpauseOnSuccessfulDeploy` | | `false` |

#### SingularityDeploy

| deploy field | config field | default value |
|--------------|--------------|---------------|
| `deployHealthTimeoutSeconds` | `deployHealthyBySeconds` | 120|
| `healthcheckProtocol`| | `HTTP`|
| `healthcheckIntervalSeconds` | `healthcheckIntervalSeconds` | 5 |
| `healthcheckTimeoutSeconds` | `healthcheckTimeoutSeconds` | 5 |
| `healthcheckPortIndex` | | 0 |
| `considerHealthyAfterRunningForSeconds` | `considerTaskHealthyAfterRunningForSeconds` | 5 |
| `loadBalancerPortIndex` | | 0 |
| `skipHealthchecksOnDeploy` | `SingularityRequest.skipHealthchecks` | `false` |
| `healthcheckMaxRetries` | `healthcheckMaxRetries` | 0 |
| `healthcheckMaxTotalTimeoutSeconds` | `healthcheckMaxTotalTimeoutSeconds` | No Timeout |
| `deployInstanceCountPerStep` | | All Instances |
| `deployStepWaitTimeMs` | | 0 |
| `autoAdvanceDeploySteps` | | `true` |
| `maxTaskRetries` | `defaultDeployMaxTaskRetries` | 0 |
| `shell` | | `false` if cmd line args are present, `true` otherwise |
| `containerInfo.type` | | `MESOS` |
