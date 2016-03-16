# Models

Models:
- [`ByteString`](#model-ByteString)
- [`CommandInfo`](#model-CommandInfo)
- [`CommandInfoOrBuilder`](#model-CommandInfoOrBuilder)
- [`ContainerInfo`](#model-ContainerInfo)
- [`ContainerInfoOrBuilder`](#model-ContainerInfoOrBuilder)
- [`Descriptor`](#model-Descriptor)
- [`DiscoveryInfo`](#model-DiscoveryInfo)
- [`DiscoveryInfoOrBuilder`](#model-DiscoveryInfoOrBuilder)
- [`DockerInfo`](#model-DockerInfo)
- [`DockerInfoOrBuilder`](#model-DockerInfoOrBuilder)
- [`EmbeddedArtifact`](#model-EmbeddedArtifact)
- [`Environment`](#model-Environment)
- [`EnvironmentOrBuilder`](#model-EnvironmentOrBuilder)
- [`ExecutorData`](#model-ExecutorData)
- [`ExecutorID`](#model-ExecutorID)
- [`ExecutorIDOrBuilder`](#model-ExecutorIDOrBuilder)
- [`ExecutorInfo`](#model-ExecutorInfo)
- [`ExecutorInfoOrBuilder`](#model-ExecutorInfoOrBuilder)
- [`ExternalArtifact`](#model-ExternalArtifact)
- [`FileDescriptor`](#model-FileDescriptor)
- [`FileOptions`](#model-FileOptions)
- [`FrameworkID`](#model-FrameworkID)
- [`FrameworkIDOrBuilder`](#model-FrameworkIDOrBuilder)
- [`HTTP`](#model-HTTP)
- [`HTTPOrBuilder`](#model-HTTPOrBuilder)
- [`HealthCheck`](#model-HealthCheck)
- [`HealthCheckOrBuilder`](#model-HealthCheckOrBuilder)
- [`Labels`](#model-Labels)
- [`LabelsOrBuilder`](#model-LabelsOrBuilder)
- [`LoadBalancerRequestId`](#model-LoadBalancerRequestId)
- [`MesosFileChunkObject`](#model-MesosFileChunkObject)
- [`MesosTaskStatisticsObject`](#model-MesosTaskStatisticsObject)
- [`MessageOptions`](#model-MessageOptions)
- [`Offer`](#model-Offer)
- [`OfferID`](#model-OfferID)
- [`OfferIDOrBuilder`](#model-OfferIDOrBuilder)
- [`Ports`](#model-Ports)
- [`PortsOrBuilder`](#model-PortsOrBuilder)
- [`Resources`](#model-Resources)
- [`S3Artifact`](#model-S3Artifact)
- [`S3ArtifactSignature`](#model-S3ArtifactSignature)
- [`SingularityBounceRequest`](#model-SingularityBounceRequest)
- [`SingularityContainerInfo`](#model-SingularityContainerInfo)
- [`SingularityDeleteRequestRequest`](#model-SingularityDeleteRequestRequest)
- [`SingularityDeploy`](#model-SingularityDeploy)
- [`SingularityDeployFailure`](#model-SingularityDeployFailure)
- [`SingularityDeployHistory`](#model-SingularityDeployHistory)
- [`SingularityDeployMarker`](#model-SingularityDeployMarker)
- [`SingularityDeployProgress`](#model-SingularityDeployProgress)
- [`SingularityDeployRequest`](#model-SingularityDeployRequest)
- [`SingularityDeployResult`](#model-SingularityDeployResult)
- [`SingularityDeployStatistics`](#model-SingularityDeployStatistics)
- [`SingularityDeployUpdate`](#model-SingularityDeployUpdate)
- [`SingularityDockerInfo`](#model-SingularityDockerInfo)
- [`SingularityDockerPortMapping`](#model-SingularityDockerPortMapping)
- [`SingularityExitCooldownRequest`](#model-SingularityExitCooldownRequest)
- [`SingularityExpiringBounce`](#model-SingularityExpiringBounce)
- [`SingularityExpiringPause`](#model-SingularityExpiringPause)
- [`SingularityExpiringScale`](#model-SingularityExpiringScale)
- [`SingularityExpiringSkipHealthchecks`](#model-SingularityExpiringSkipHealthchecks)
- [`SingularityHostState`](#model-SingularityHostState)
- [`SingularityKillTaskRequest`](#model-SingularityKillTaskRequest)
- [`SingularityLoadBalancerUpdate`](#model-SingularityLoadBalancerUpdate)
- [`SingularityMachineChangeRequest`](#model-SingularityMachineChangeRequest)
- [`SingularityMachineStateHistoryUpdate`](#model-SingularityMachineStateHistoryUpdate)
- [`SingularityPauseRequest`](#model-SingularityPauseRequest)
- [`SingularityPendingDeploy`](#model-SingularityPendingDeploy)
- [`SingularityPendingRequest`](#model-SingularityPendingRequest)
- [`SingularityPendingTask`](#model-SingularityPendingTask)
- [`SingularityPendingTaskId`](#model-SingularityPendingTaskId)
- [`SingularityRack`](#model-SingularityRack)
- [`SingularityRequest`](#model-SingularityRequest)
- [`SingularityRequestCleanup`](#model-SingularityRequestCleanup)
- [`SingularityRequestDeployState`](#model-SingularityRequestDeployState)
- [`SingularityRequestHistory`](#model-SingularityRequestHistory)
- [`SingularityRequestParent`](#model-SingularityRequestParent)
- [`SingularityRunNowRequest`](#model-SingularityRunNowRequest)
- [`SingularitySandbox`](#model-SingularitySandbox)
- [`SingularitySandboxFile`](#model-SingularitySandboxFile)
- [`SingularityScaleRequest`](#model-SingularityScaleRequest)
- [`SingularityShellCommand`](#model-SingularityShellCommand)
- [`SingularitySkipHealthchecksRequest`](#model-SingularitySkipHealthchecksRequest)
- [`SingularitySlave`](#model-SingularitySlave)
- [`SingularityState`](#model-SingularityState)
- [`SingularityTask`](#model-SingularityTask)
- [`SingularityTaskCleanup`](#model-SingularityTaskCleanup)
- [`SingularityTaskHealthcheckResult`](#model-SingularityTaskHealthcheckResult)
- [`SingularityTaskHistory`](#model-SingularityTaskHistory)
- [`SingularityTaskHistoryUpdate`](#model-SingularityTaskHistoryUpdate)
- [`SingularityTaskId`](#model-SingularityTaskId)
- [`SingularityTaskIdHistory`](#model-SingularityTaskIdHistory)
- [`SingularityTaskRequest`](#model-SingularityTaskRequest)
- [`SingularityTaskShellCommandHistory`](#model-SingularityTaskShellCommandHistory)
- [`SingularityTaskShellCommandRequest`](#model-SingularityTaskShellCommandRequest)
- [`SingularityTaskShellCommandRequestId`](#model-SingularityTaskShellCommandRequestId)
- [`SingularityTaskShellCommandUpdate`](#model-SingularityTaskShellCommandUpdate)
- [`SingularityUnpauseRequest`](#model-SingularityUnpauseRequest)
- [`SingularityUpdatePendingDeployRequest`](#model-SingularityUpdatePendingDeployRequest)
- [`SingularityVolume`](#model-SingularityVolume)
- [`SingularityWebhook`](#model-SingularityWebhook)
- [`SlaveID`](#model-SlaveID)
- [`SlaveIDOrBuilder`](#model-SlaveIDOrBuilder)
- [`TaskID`](#model-TaskID)
- [`TaskIDOrBuilder`](#model-TaskIDOrBuilder)
- [`TaskInfo`](#model-TaskInfo)
- [`UnknownFieldSet`](#model-UnknownFieldSet)



## Data Types

## <a name="model-ByteString"></a> ByteString

| name | type | required | description |
|------|------|----------|-------------|
| validUtf8 | boolean | optional |  |
| empty | boolean | optional |  |


## <a name="model-CommandInfo"></a> CommandInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [CommandInfo](#model-CommandInfo) | optional |  |
| urisOrBuilderList | [List[? extends org.apache.mesos.Protos$CommandInfo$URIOrBuilder]](#model-List[? extends org.apache.mesos.Protos$CommandInfo$URIOrBuilder]) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$CommandInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$CommandInfo&gt;) | optional |  |
| urisCount | int | optional |  |
| argumentsCount | int | optional |  |
| argumentsList | Array[string] | optional |  |
| containerOrBuilder | [ContainerInfoOrBuilder](#model-ContainerInfoOrBuilder) | optional |  |
| user | string | optional |  |
| container | [ContainerInfo](#model-ContainerInfo) | optional |  |
| value | string | optional |  |
| initialized | boolean | optional |  |
| environment | [Environment](#model-Environment) | optional |  |
| userBytes | [ByteString](#model-ByteString) | optional |  |
| shell | boolean | optional |  |
| serializedSize | int | optional |  |
| urisList | [List[URI]](#model-List[URI]) | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| environmentOrBuilder | [EnvironmentOrBuilder](#model-EnvironmentOrBuilder) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-CommandInfoOrBuilder"></a> CommandInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| urisOrBuilderList | [List[? extends org.apache.mesos.Protos$CommandInfo$URIOrBuilder]](#model-List[? extends org.apache.mesos.Protos$CommandInfo$URIOrBuilder]) | optional |  |
| urisCount | int | optional |  |
| argumentsCount | int | optional |  |
| argumentsList | Array[string] | optional |  |
| containerOrBuilder | [ContainerInfoOrBuilder](#model-ContainerInfoOrBuilder) | optional |  |
| user | string | optional |  |
| container | [ContainerInfo](#model-ContainerInfo) | optional |  |
| value | string | optional |  |
| environment | [Environment](#model-Environment) | optional |  |
| userBytes | [ByteString](#model-ByteString) | optional |  |
| shell | boolean | optional |  |
| urisList | [List[URI]](#model-List[URI]) | optional |  |
| environmentOrBuilder | [EnvironmentOrBuilder](#model-EnvironmentOrBuilder) | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-ContainerInfo"></a> ContainerInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [ContainerInfo](#model-ContainerInfo) | optional |  |
| type | [Type](#model-Type) | optional |  Allowable values: DOCKER, MESOS |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo&gt;) | optional |  |
| hostname | string | optional |  |
| dockerOrBuilder | [DockerInfoOrBuilder](#model-DockerInfoOrBuilder) | optional |  |
| initialized | boolean | optional |  |
| volumesCount | int | optional |  |
| serializedSize | int | optional |  |
| volumesList | [List[Volume]](#model-List[Volume]) | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| hostnameBytes | [ByteString](#model-ByteString) | optional |  |
| volumesOrBuilderList | [List[? extends org.apache.mesos.Protos$VolumeOrBuilder]](#model-List[? extends org.apache.mesos.Protos$VolumeOrBuilder]) | optional |  |
| docker | [DockerInfo](#model-DockerInfo) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-ContainerInfoOrBuilder"></a> ContainerInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| type | [Type](#model-Type) | optional |  Allowable values: DOCKER, MESOS |
| hostname | string | optional |  |
| dockerOrBuilder | [DockerInfoOrBuilder](#model-DockerInfoOrBuilder) | optional |  |
| volumesCount | int | optional |  |
| volumesList | [List[Volume]](#model-List[Volume]) | optional |  |
| hostnameBytes | [ByteString](#model-ByteString) | optional |  |
| docker | [DockerInfo](#model-DockerInfo) | optional |  |
| volumesOrBuilderList | [List[? extends org.apache.mesos.Protos$VolumeOrBuilder]](#model-List[? extends org.apache.mesos.Protos$VolumeOrBuilder]) | optional |  |


## <a name="model-Descriptor"></a> Descriptor

| name | type | required | description |
|------|------|----------|-------------|
| enumTypes | [List[EnumDescriptor]](#model-List[EnumDescriptor]) | optional |  |
| fullName | string | optional |  |
| containingType | [Descriptor](#model-Descriptor) | optional |  |
| file | [FileDescriptor](#model-FileDescriptor) | optional |  |
| extensions | [List[FieldDescriptor]](#model-List[FieldDescriptor]) | optional |  |
| options | [MessageOptions](#model-MessageOptions) | optional |  |
| fields | [List[FieldDescriptor]](#model-List[FieldDescriptor]) | optional |  |
| name | string | optional |  |
| index | int | optional |  |
| nestedTypes | [List[Descriptor]](#model-List[Descriptor]) | optional |  |


## <a name="model-DiscoveryInfo"></a> DiscoveryInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [DiscoveryInfo](#model-DiscoveryInfo) | optional |  |
| location | string | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$DiscoveryInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$DiscoveryInfo&gt;) | optional |  |
| labelsOrBuilder | [LabelsOrBuilder](#model-LabelsOrBuilder) | optional |  |
| versionBytes | [ByteString](#model-ByteString) | optional |  |
| labels | [Labels](#model-Labels) | optional |  |
| locationBytes | [ByteString](#model-ByteString) | optional |  |
| initialized | boolean | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| name | string | optional |  |
| environment | string | optional |  |
| ports | [Ports](#model-Ports) | optional |  |
| visibility | [Visibility](#model-Visibility) | optional |  Allowable values: FRAMEWORK, CLUSTER, EXTERNAL |
| environmentBytes | [ByteString](#model-ByteString) | optional |  |
| serializedSize | int | optional |  |
| portsOrBuilder | [PortsOrBuilder](#model-PortsOrBuilder) | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| version | string | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-DiscoveryInfoOrBuilder"></a> DiscoveryInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| location | string | optional |  |
| labelsOrBuilder | [LabelsOrBuilder](#model-LabelsOrBuilder) | optional |  |
| versionBytes | [ByteString](#model-ByteString) | optional |  |
| labels | [Labels](#model-Labels) | optional |  |
| locationBytes | [ByteString](#model-ByteString) | optional |  |
| name | string | optional |  |
| environment | string | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| ports | [Ports](#model-Ports) | optional |  |
| visibility | [Visibility](#model-Visibility) | optional |  Allowable values: FRAMEWORK, CLUSTER, EXTERNAL |
| environmentBytes | [ByteString](#model-ByteString) | optional |  |
| portsOrBuilder | [PortsOrBuilder](#model-PortsOrBuilder) | optional |  |
| version | string | optional |  |


## <a name="model-DockerInfo"></a> DockerInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [DockerInfo](#model-DockerInfo) | optional |  |
| portMappingsOrBuilderList | [List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]) | optional |  |
| parametersList | [List[Parameter]](#model-List[Parameter]) | optional |  |
| parametersOrBuilderList | [List[? extends org.apache.mesos.Protos$ParameterOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ParameterOrBuilder]) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo$DockerInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo$DockerInfo&gt;) | optional |  |
| forcePullImage | boolean | optional |  |
| imageBytes | [ByteString](#model-ByteString) | optional |  |
| initialized | boolean | optional |  |
| privileged | boolean | optional |  |
| portMappingsCount | int | optional |  |
| parametersCount | int | optional |  |
| serializedSize | int | optional |  |
| network | [Network](#model-Network) | optional |  Allowable values: HOST, BRIDGE, NONE |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| portMappingsList | [List[PortMapping]](#model-List[PortMapping]) | optional |  |
| image | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-DockerInfoOrBuilder"></a> DockerInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| portMappingsOrBuilderList | [List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]) | optional |  |
| parametersOrBuilderList | [List[? extends org.apache.mesos.Protos$ParameterOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ParameterOrBuilder]) | optional |  |
| parametersList | [List[Parameter]](#model-List[Parameter]) | optional |  |
| forcePullImage | boolean | optional |  |
| imageBytes | [ByteString](#model-ByteString) | optional |  |
| privileged | boolean | optional |  |
| parametersCount | int | optional |  |
| portMappingsCount | int | optional |  |
| network | [Network](#model-Network) | optional |  Allowable values: HOST, BRIDGE, NONE |
| portMappingsList | [List[PortMapping]](#model-List[PortMapping]) | optional |  |
| image | string | optional |  |


## <a name="model-EmbeddedArtifact"></a> EmbeddedArtifact

| name | type | required | description |
|------|------|----------|-------------|
| md5sum | string | optional |  |
| filename | string | optional |  |
| name | string | optional |  |
| content | [Array[byte]](#model-byte) | optional |  |


## <a name="model-Environment"></a> Environment

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Environment](#model-Environment) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Environment&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Environment&gt;) | optional |  |
| initialized | boolean | optional |  |
| variablesCount | int | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| variablesOrBuilderList | [List[? extends org.apache.mesos.Protos$Environment$VariableOrBuilder]](#model-List[? extends org.apache.mesos.Protos$Environment$VariableOrBuilder]) | optional |  |
| variablesList | [List[Variable]](#model-List[Variable]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-EnvironmentOrBuilder"></a> EnvironmentOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| variablesCount | int | optional |  |
| variablesOrBuilderList | [List[? extends org.apache.mesos.Protos$Environment$VariableOrBuilder]](#model-List[? extends org.apache.mesos.Protos$Environment$VariableOrBuilder]) | optional |  |
| variablesList | [List[Variable]](#model-List[Variable]) | optional |  |


## <a name="model-ExecutorData"></a> ExecutorData

| name | type | required | description |
|------|------|----------|-------------|
| skipLogrotateAndCompress | boolean | optional |  |
| loggingExtraFields | [Map[string,string]](#model-Map[string,string]) | optional |  |
| embeddedArtifacts | [Array[EmbeddedArtifact]](#model-EmbeddedArtifact) | optional |  |
| s3Artifacts | [Array[S3Artifact]](#model-S3Artifact) | optional |  |
| successfulExitCodes | Array[int] | optional |  |
| runningSentinel | string | optional |  |
| maxOpenFiles | int | optional |  |
| externalArtifacts | [Array[ExternalArtifact]](#model-ExternalArtifact) | optional |  |
| user | string | optional |  |
| preserveTaskSandboxAfterFinish | boolean | optional |  |
| extraCmdLineArgs | Array[string] | optional |  |
| loggingTag | string | optional |  |
| loggingS3Bucket | string | optional |  |
| sigKillProcessesAfterMillis | long | optional |  |
| maxTaskThreads | int | optional |  |
| s3ArtifactSignatures | [Array[S3ArtifactSignature]](#model-S3ArtifactSignature) | optional |  |
| cmd | string | optional |  |


## <a name="model-ExecutorID"></a> ExecutorID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [ExecutorID](#model-ExecutorID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorID&gt;) | optional |  |
| initialized | boolean | optional |  |
| value | string | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-ExecutorIDOrBuilder"></a> ExecutorIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-ExecutorInfo"></a> ExecutorInfo

| name | type | required | description |
|------|------|----------|-------------|
| commandOrBuilder | [CommandInfoOrBuilder](#model-CommandInfoOrBuilder) | optional |  |
| defaultInstanceForType | [ExecutorInfo](#model-ExecutorInfo) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorInfo&gt;) | optional |  |
| resourcesOrBuilderList | [List[? extends org.apache.mesos.Protos$ResourceOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ResourceOrBuilder]) | optional |  |
| data | [ByteString](#model-ByteString) | optional |  |
| source | string | optional |  |
| containerOrBuilder | [ContainerInfoOrBuilder](#model-ContainerInfoOrBuilder) | optional |  |
| executorId | [ExecutorID](#model-ExecutorID) | optional |  |
| container | [ContainerInfo](#model-ContainerInfo) | optional |  |
| initialized | boolean | optional |  |
| name | string | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| frameworkId | [FrameworkID](#model-FrameworkID) | optional |  |
| sourceBytes | [ByteString](#model-ByteString) | optional |  |
| command | [CommandInfo](#model-CommandInfo) | optional |  |
| frameworkIdOrBuilder | [FrameworkIDOrBuilder](#model-FrameworkIDOrBuilder) | optional |  |
| executorIdOrBuilder | [ExecutorIDOrBuilder](#model-ExecutorIDOrBuilder) | optional |  |
| serializedSize | int | optional |  |
| resourcesList | [List[Resource]](#model-List[Resource]) | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| discovery | [DiscoveryInfo](#model-DiscoveryInfo) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| resourcesCount | int | optional |  |
| initializationErrorString | string | optional |  |
| discoveryOrBuilder | [DiscoveryInfoOrBuilder](#model-DiscoveryInfoOrBuilder) | optional |  |


## <a name="model-ExecutorInfoOrBuilder"></a> ExecutorInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| commandOrBuilder | [CommandInfoOrBuilder](#model-CommandInfoOrBuilder) | optional |  |
| resourcesOrBuilderList | [List[? extends org.apache.mesos.Protos$ResourceOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ResourceOrBuilder]) | optional |  |
| data | [ByteString](#model-ByteString) | optional |  |
| source | string | optional |  |
| containerOrBuilder | [ContainerInfoOrBuilder](#model-ContainerInfoOrBuilder) | optional |  |
| executorId | [ExecutorID](#model-ExecutorID) | optional |  |
| container | [ContainerInfo](#model-ContainerInfo) | optional |  |
| name | string | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| command | [CommandInfo](#model-CommandInfo) | optional |  |
| sourceBytes | [ByteString](#model-ByteString) | optional |  |
| frameworkId | [FrameworkID](#model-FrameworkID) | optional |  |
| frameworkIdOrBuilder | [FrameworkIDOrBuilder](#model-FrameworkIDOrBuilder) | optional |  |
| executorIdOrBuilder | [ExecutorIDOrBuilder](#model-ExecutorIDOrBuilder) | optional |  |
| resourcesList | [List[Resource]](#model-List[Resource]) | optional |  |
| discovery | [DiscoveryInfo](#model-DiscoveryInfo) | optional |  |
| resourcesCount | int | optional |  |
| discoveryOrBuilder | [DiscoveryInfoOrBuilder](#model-DiscoveryInfoOrBuilder) | optional |  |


## <a name="model-ExternalArtifact"></a> ExternalArtifact

| name | type | required | description |
|------|------|----------|-------------|
| md5sum | string | optional |  |
| url | string | optional |  |
| filename | string | optional |  |
| filesize | long | optional |  |
| name | string | optional |  |


## <a name="model-FileDescriptor"></a> FileDescriptor

| name | type | required | description |
|------|------|----------|-------------|
| enumTypes | [List[EnumDescriptor]](#model-List[EnumDescriptor]) | optional |  |
| publicDependencies | [List[FileDescriptor]](#model-List[FileDescriptor]) | optional |  |
| extensions | [List[FieldDescriptor]](#model-List[FieldDescriptor]) | optional |  |
| services | [List[ServiceDescriptor]](#model-List[ServiceDescriptor]) | optional |  |
| options | [FileOptions](#model-FileOptions) | optional |  |
| messageTypes | [List[Descriptor]](#model-List[Descriptor]) | optional |  |
| name | string | optional |  |
| dependencies | [List[FileDescriptor]](#model-List[FileDescriptor]) | optional |  |
| package | string | optional |  |


## <a name="model-FileOptions"></a> FileOptions

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [FileOptions](#model-FileOptions) | optional |  |
| javaMultipleFiles | boolean | optional |  |
| optimizeFor | [OptimizeMode](#model-OptimizeMode) | optional |  Allowable values: SPEED, CODE_SIZE, LITE_RUNTIME |
| parserForType | [com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$FileOptions&gt;](#model-com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$FileOptions&gt;) | optional |  |
| javaPackageBytes | [ByteString](#model-ByteString) | optional |  |
| goPackageBytes | [ByteString](#model-ByteString) | optional |  |
| javaGenericServices | boolean | optional |  |
| uninterpretedOptionCount | int | optional |  |
| javaOuterClassnameBytes | [ByteString](#model-ByteString) | optional |  |
| initialized | boolean | optional |  |
| javaOuterClassname | string | optional |  |
| pyGenericServices | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| goPackage | string | optional |  |
| uninterpretedOptionList | [List[UninterpretedOption]](#model-List[UninterpretedOption]) | optional |  |
| javaPackage | string | optional |  |
| javaGenerateEqualsAndHash | boolean | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| uninterpretedOptionOrBuilderList | [List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]](#model-List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]) | optional |  |
| initializationErrorString | string | optional |  |
| ccGenericServices | boolean | optional |  |


## <a name="model-FrameworkID"></a> FrameworkID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [FrameworkID](#model-FrameworkID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$FrameworkID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$FrameworkID&gt;) | optional |  |
| initialized | boolean | optional |  |
| value | string | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-FrameworkIDOrBuilder"></a> FrameworkIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-HTTP"></a> HTTP

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [HTTP](#model-HTTP) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$HealthCheck$HTTP&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$HealthCheck$HTTP&gt;) | optional |  |
| pathBytes | [ByteString](#model-ByteString) | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |
| statusesCount | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| path | string | optional |  |
| port | int | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| statusesList | Array[int] | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-HTTPOrBuilder"></a> HTTPOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| pathBytes | [ByteString](#model-ByteString) | optional |  |
| statusesCount | int | optional |  |
| port | int | optional |  |
| path | string | optional |  |
| statusesList | Array[int] | optional |  |


## <a name="model-HealthCheck"></a> HealthCheck

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [HealthCheck](#model-HealthCheck) | optional |  |
| commandOrBuilder | [CommandInfoOrBuilder](#model-CommandInfoOrBuilder) | optional |  |
| gracePeriodSeconds | double | optional |  |
| httpOrBuilder | [HTTPOrBuilder](#model-HTTPOrBuilder) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$HealthCheck&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$HealthCheck&gt;) | optional |  |
| consecutiveFailures | int | optional |  |
| intervalSeconds | double | optional |  |
| initialized | boolean | optional |  |
| command | [CommandInfo](#model-CommandInfo) | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| timeoutSeconds | double | optional |  |
| http | [HTTP](#model-HTTP) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| delaySeconds | double | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-HealthCheckOrBuilder"></a> HealthCheckOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| commandOrBuilder | [CommandInfoOrBuilder](#model-CommandInfoOrBuilder) | optional |  |
| gracePeriodSeconds | double | optional |  |
| httpOrBuilder | [HTTPOrBuilder](#model-HTTPOrBuilder) | optional |  |
| consecutiveFailures | int | optional |  |
| intervalSeconds | double | optional |  |
| command | [CommandInfo](#model-CommandInfo) | optional |  |
| timeoutSeconds | double | optional |  |
| http | [HTTP](#model-HTTP) | optional |  |
| delaySeconds | double | optional |  |


## <a name="model-Labels"></a> Labels

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Labels](#model-Labels) | optional |  |
| labelsList | [List[Label]](#model-List[Label]) | optional |  |
| labelsOrBuilderList | [List[? extends org.apache.mesos.Protos$LabelOrBuilder]](#model-List[? extends org.apache.mesos.Protos$LabelOrBuilder]) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Labels&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Labels&gt;) | optional |  |
| initialized | boolean | optional |  |
| labelsCount | int | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-LabelsOrBuilder"></a> LabelsOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| labelsList | [List[Label]](#model-List[Label]) | optional |  |
| labelsOrBuilderList | [List[? extends org.apache.mesos.Protos$LabelOrBuilder]](#model-List[? extends org.apache.mesos.Protos$LabelOrBuilder]) | optional |  |
| labelsCount | int | optional |  |


## <a name="model-LoadBalancerRequestId"></a> LoadBalancerRequestId

| name | type | required | description |
|------|------|----------|-------------|
| requestType | [LoadBalancerRequestType](#model-LoadBalancerRequestType) | optional |  Allowable values: ADD, REMOVE, DEPLOY, DELETE |
| attemptNumber | int | optional |  |
| id | string | optional |  |


## <a name="model-MesosFileChunkObject"></a> MesosFileChunkObject

| name | type | required | description |
|------|------|----------|-------------|
| nextOffset | long | optional |  |
| data | string | optional |  |
| offset | long | optional |  |


## <a name="model-MesosTaskStatisticsObject"></a> MesosTaskStatisticsObject

| name | type | required | description |
|------|------|----------|-------------|
| memFileBytes | long | optional |  |
| memLimitBytes | long | optional |  |
| cpusThrottledTimeSecs | double | optional |  |
| cpusSystemTimeSecs | double | optional |  |
| memRssBytes | long | optional |  |
| memAnonBytes | long | optional |  |
| memMappedFileBytes | long | optional |  |
| cpusLimit | int | optional |  |
| timestamp | double | optional |  |
| cpusNrPeriods | long | optional |  |
| cpusUserTimeSecs | double | optional |  |
| cpusNrThrottled | long | optional |  |


## <a name="model-MessageOptions"></a> MessageOptions

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [MessageOptions](#model-MessageOptions) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$MessageOptions&gt;](#model-com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$MessageOptions&gt;) | optional |  |
| uninterpretedOptionCount | int | optional |  |
| initialized | boolean | optional |  |
| noStandardDescriptorAccessor | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| uninterpretedOptionList | [List[UninterpretedOption]](#model-List[UninterpretedOption]) | optional |  |
| messageSetWireFormat | boolean | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| uninterpretedOptionOrBuilderList | [List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]](#model-List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-Offer"></a> Offer

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Offer](#model-Offer) | optional |  |
| executorIdsOrBuilderList | [List[? extends org.apache.mesos.Protos$ExecutorIDOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ExecutorIDOrBuilder]) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Offer&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Offer&gt;) | optional |  |
| slaveIdOrBuilder | [SlaveIDOrBuilder](#model-SlaveIDOrBuilder) | optional |  |
| executorIdsCount | int | optional |  |
| resourcesOrBuilderList | [List[? extends org.apache.mesos.Protos$ResourceOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ResourceOrBuilder]) | optional |  |
| executorIdsList | [List[ExecutorID]](#model-List[ExecutorID]) | optional |  |
| hostname | string | optional |  |
| attributesCount | int | optional |  |
| initialized | boolean | optional |  |
| attributesList | [List[Attribute]](#model-List[Attribute]) | optional |  |
| idOrBuilder | [OfferIDOrBuilder](#model-OfferIDOrBuilder) | optional |  |
| frameworkId | [FrameworkID](#model-FrameworkID) | optional |  |
| frameworkIdOrBuilder | [FrameworkIDOrBuilder](#model-FrameworkIDOrBuilder) | optional |  |
| serializedSize | int | optional |  |
| resourcesList | [List[Resource]](#model-List[Resource]) | optional |  |
| slaveId | [SlaveID](#model-SlaveID) | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| hostnameBytes | [ByteString](#model-ByteString) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| attributesOrBuilderList | [List[? extends org.apache.mesos.Protos$AttributeOrBuilder]](#model-List[? extends org.apache.mesos.Protos$AttributeOrBuilder]) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| resourcesCount | int | optional |  |
| id | [OfferID](#model-OfferID) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-OfferID"></a> OfferID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [OfferID](#model-OfferID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$OfferID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$OfferID&gt;) | optional |  |
| initialized | boolean | optional |  |
| value | string | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-OfferIDOrBuilder"></a> OfferIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-Ports"></a> Ports

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Ports](#model-Ports) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Ports&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Ports&gt;) | optional |  |
| initialized | boolean | optional |  |
| portsCount | int | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| portsOrBuilderList | [List[? extends org.apache.mesos.Protos$PortOrBuilder]](#model-List[? extends org.apache.mesos.Protos$PortOrBuilder]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| portsList | [List[Port]](#model-List[Port]) | optional |  |
| initializationErrorString | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-PortsOrBuilder"></a> PortsOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| portsCount | int | optional |  |
| portsOrBuilderList | [List[? extends org.apache.mesos.Protos$PortOrBuilder]](#model-List[? extends org.apache.mesos.Protos$PortOrBuilder]) | optional |  |
| portsList | [List[Port]](#model-List[Port]) | optional |  |


## <a name="model-Resources"></a> Resources

| name | type | required | description |
|------|------|----------|-------------|
| numPorts | int | optional |  |
| memoryMb | double | optional |  |
| cpus | double | optional |  |


## <a name="model-S3Artifact"></a> S3Artifact

| name | type | required | description |
|------|------|----------|-------------|
| s3Bucket | string | optional |  |
| md5sum | string | optional |  |
| filename | string | optional |  |
| filesize | long | optional |  |
| s3ObjectKey | string | optional |  |
| name | string | optional |  |


## <a name="model-S3ArtifactSignature"></a> S3ArtifactSignature

| name | type | required | description |
|------|------|----------|-------------|
| s3Bucket | string | optional |  |
| md5sum | string | optional |  |
| filename | string | optional |  |
| filesize | long | optional |  |
| s3ObjectKey | string | optional |  |
| name | string | optional |  |
| artifactFilename | string | optional |  |


## <a name="model-SingularityBounceRequest"></a> SingularityBounceRequest

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | Instruct replacement tasks for this bounce only to skip healthchecks |
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |
| incremental | boolean | optional | If present and set to true, old tasks will be killed as soon as replacement tasks are available, instead of waiting for all replacement tasks to be healthy |


## <a name="model-SingularityContainerInfo"></a> SingularityContainerInfo

| name | type | required | description |
|------|------|----------|-------------|
| type | [SingularityContainerType](#model-SingularityContainerType) | optional |  Allowable values: MESOS, DOCKER |
| volumes | [Array[SingularityVolume]](#model-SingularityVolume) | optional |  |
| docker | [SingularityDockerInfo](#model-SingularityDockerInfo) | optional |  |


## <a name="model-SingularityDeleteRequestRequest"></a> SingularityDeleteRequestRequest

| name | type | required | description |
|------|------|----------|-------------|
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


## <a name="model-SingularityDeploy"></a> SingularityDeploy

| name | type | required | description |
|------|------|----------|-------------|
| customExecutorId | string | optional | Custom Mesos executor id. |
| resources | [com.hubspot.mesos.Resources](#model-com.hubspot.mesos.Resources) | optional | Resources required for this deploy. |
| uris | Array[string] | optional | List of URIs to download before executing the deploy command. |
| containerInfo | [SingularityContainerInfo](#model-SingularityContainerInfo) | optional | Container information for deployment into a container. |
| arguments | Array[string] | optional | Command arguments. |
| autoAdvanceDeploySteps | boolean | optional | automatically advance to the next target instance count after `deployStepWaitTimeMs` seconds |
| serviceBasePath | string | optional | The base path for the API exposed by the deploy. Used in conjunction with the Load balancer API. |
| customExecutorUser | string | optional | User to run custom executor as |
| customExecutorSource | string | optional | Custom Mesos executor source. |
| metadata | [Map[string,string]](#model-Map[string,string]) | optional | Map of metadata key/value pairs associated with the deployment. |
| healthcheckTimeoutSeconds | long | optional | Single healthcheck HTTP timeout in seconds. |
| healthcheckMaxRetries | int | optional | Maximum number of times to retry an individual healthcheck before failing the deploy. |
| healthcheckPortIndex | int | optional | Perform healthcheck on this dynamically allocated port (e.g. 0 for first port), defaults to first port |
| healthcheckProtocol | [HealthcheckProtocol](#model-HealthcheckProtocol) | optional | Healthcheck protocol - HTTP or HTTPS |
| healthcheckMaxTotalTimeoutSeconds | long | optional | Maximum amount of time to wait before failing a deploy for healthchecks to pass. |
| labels | [Map[string,string]](#model-Map[string,string]) | optional | Labels for tasks associated with this deploy |
| healthcheckUri | string | optional | Deployment Healthcheck URI, if specified will be called after TASK_RUNNING. |
| requestId | string | required | Singularity Request Id which is associated with this deploy. |
| loadBalancerGroups | [Set](#model-Set) | optional | List of load balancer groups associated with this deployment. |
| deployStepWaitTimeMs | int | optional | wait this long between deploy steps |
| skipHealthchecksOnDeploy | boolean | optional | Allows skipping of health checks when deploying. |
| healthcheckIntervalSeconds | long | optional | Time to wait after a failed healthcheck to try again in seconds. |
| command | string | optional | Command to execute for this deployment. |
| executorData | [ExecutorData](#model-ExecutorData) | optional | Executor specific information |
| timestamp | long | optional | Deploy timestamp. |
| deployInstanceCountPerStep | int | optional | deploy this many instances at a time |
| considerHealthyAfterRunningForSeconds | long | optional | Number of seconds that a service must be healthy to consider the deployment to be successful. |
| loadBalancerOptions | [Map[string,Object]](#model-Map[string,Object]) | optional | Map (Key/Value) of options for the load balancer. |
| maxTaskRetries | int | optional | allowed at most this many failed tasks to be retried before failing the deploy |
| loadBalancerPortIndex | int | optional | Send this port to the load balancer api (e.g. 0 for first port), defaults to first port |
| customExecutorCmd | string | optional | Custom Mesos executor |
| env | [Map[string,string]](#model-Map[string,string]) | optional | Map of environment variable definitions. |
| customExecutorResources | [Resources](#model-Resources) | optional | Resources to allocate for custom mesos executor |
| version | string | optional | Deploy version |
| id | string | required | Singularity deploy id. |
| deployHealthTimeoutSeconds | long | optional | Number of seconds that Singularity waits for this service to become healthy (for it to download artifacts, start running, and optionally pass healthchecks.) |


## <a name="model-SingularityDeployFailure"></a> SingularityDeployFailure

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| message | string | optional |  |
| reason | [SingularityDeployFailureReason](#model-SingularityDeployFailureReason) | optional |  Allowable values: TASK_FAILED_ON_STARTUP, TASK_FAILED_HEALTH_CHECKS, TASK_COULD_NOT_BE_SCHEDULED, TASK_NEVER_ENTERED_RUNNING, TASK_EXPECTED_RUNNING_FINISHED, DEPLOY_CANCELLED, DEPLOY_OVERDUE, FAILED_TO_SAVE_DEPLOY_STATE, LOAD_BALANCER_UPDATE_FAILED, PENDING_DEPLOY_REMOVED |


## <a name="model-SingularityDeployHistory"></a> SingularityDeployHistory

| name | type | required | description |
|------|------|----------|-------------|
| deploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| deployStatistics | [SingularityDeployStatistics](#model-SingularityDeployStatistics) | optional |  |
| deployResult | [SingularityDeployResult](#model-SingularityDeployResult) | optional |  |
| deployMarker | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |


## <a name="model-SingularityDeployMarker"></a> SingularityDeployMarker

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| requestId | string | optional |  |
| message | string | optional |  |
| timestamp | long | optional |  |
| deployId | string | optional |  |


## <a name="model-SingularityDeployProgress"></a> SingularityDeployProgress

| name | type | required | description |
|------|------|----------|-------------|
| autoAdvanceDeploySteps | boolean | optional |  |
| stepComplete | boolean | optional |  |
| deployStepWaitTimeMs | long | optional |  |
| timestamp | long | optional |  |
| deployInstanceCountPerStep | int | optional |  |
| failedDeployTasks | [Set](#model-Set) | optional |  |
| targetActiveInstances | int | optional |  |


## <a name="model-SingularityDeployRequest"></a> SingularityDeployRequest

| name | type | required | description |
|------|------|----------|-------------|
| unpauseOnSuccessfulDeploy | boolean | optional | If deploy is successful, also unpause the request |
| deploy | [SingularityDeploy](#model-SingularityDeploy) | required | The Singularity deploy object, containing all the required details about the Deploy |
| message | string | optional | A message to show users about this deploy (metadata) |


## <a name="model-SingularityDeployResult"></a> SingularityDeployResult

| name | type | required | description |
|------|------|----------|-------------|
| lbUpdate | [SingularityLoadBalancerUpdate](#model-SingularityLoadBalancerUpdate) | optional |  |
| deployState | [DeployState](#model-DeployState) | optional |  Allowable values: SUCCEEDED, FAILED_INTERNAL_STATE, CANCELING, WAITING, OVERDUE, FAILED, CANCELED |
| deployFailures | [Array[SingularityDeployFailure]](#model-SingularityDeployFailure) | optional |  |
| message | string | optional |  |
| timestamp | long | optional |  |


## <a name="model-SingularityDeployStatistics"></a> SingularityDeployStatistics

| name | type | required | description |
|------|------|----------|-------------|
| lastTaskState | [ExtendedTaskState](#model-ExtendedTaskState) | optional |  |
| numFailures | int | optional |  |
| numTasks | int | optional |  |
| averageRuntimeMillis | long | optional |  |
| lastFinishAt | long | optional |  |
| requestId | string | optional |  |
| deployId | string | optional |  |
| numSequentialRetries | int | optional |  |
| numSuccess | int | optional |  |
| instanceSequentialFailureTimestamps | [com.google.common.collect.ListMultimap&lt;java.lang.Integer, java.lang.Long&gt;](#model-com.google.common.collect.ListMultimap&lt;java.lang.Integer, java.lang.Long&gt;) | optional |  |


## <a name="model-SingularityDeployUpdate"></a> SingularityDeployUpdate

| name | type | required | description |
|------|------|----------|-------------|
| deploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| deployResult | [SingularityDeployResult](#model-SingularityDeployResult) | optional |  |
| eventType | [DeployEventType](#model-DeployEventType) | optional |  Allowable values: STARTING, FINISHED |
| deployMarker | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |


## <a name="model-SingularityDockerInfo"></a> SingularityDockerInfo

| name | type | required | description |
|------|------|----------|-------------|
| parameters | [Map[string,string]](#model-Map[string,string]) | optional |  |
| forcePullImage | boolean | optional |  |
| privileged | boolean | optional |  |
| network | [SingularityDockerNetworkType](#model-SingularityDockerNetworkType) | optional |  |
| portMappings | [Array[SingularityDockerPortMapping]](#model-SingularityDockerPortMapping) | optional |  |
| image | string | optional |  |


## <a name="model-SingularityDockerPortMapping"></a> SingularityDockerPortMapping

| name | type | required | description |
|------|------|----------|-------------|
| hostPort | int | optional |  |
| containerPort | int | optional |  |
| containerPortType | [SingularityPortMappingType](#model-SingularityPortMappingType) | optional |  Allowable values: LITERAL, FROM_OFFER |
| protocol | string | optional |  |
| hostPortType | [SingularityPortMappingType](#model-SingularityPortMappingType) | optional |  Allowable values: LITERAL, FROM_OFFER |


## <a name="model-SingularityExitCooldownRequest"></a> SingularityExitCooldownRequest

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | Instruct new tasks that are scheduled immediately while executing cooldown to skip healthchecks |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


## <a name="model-SingularityExpiringBounce"></a> SingularityExpiringBounce

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| requestId | string | optional |  |
| startMillis | long | optional |  |
| deployId | string | optional |  |
| actionId | string | optional |  |
| expiringAPIRequestObject | [T](#model-T) | optional |  |


## <a name="model-SingularityExpiringPause"></a> SingularityExpiringPause

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| requestId | string | optional |  |
| startMillis | long | optional |  |
| actionId | string | optional |  |
| expiringAPIRequestObject | [T](#model-T) | optional |  |


## <a name="model-SingularityExpiringScale"></a> SingularityExpiringScale

| name | type | required | description |
|------|------|----------|-------------|
| revertToInstances | int | optional |  |
| user | string | optional |  |
| requestId | string | optional |  |
| startMillis | long | optional |  |
| actionId | string | optional |  |
| expiringAPIRequestObject | [T](#model-T) | optional |  |


## <a name="model-SingularityExpiringSkipHealthchecks"></a> SingularityExpiringSkipHealthchecks

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| requestId | string | optional |  |
| startMillis | long | optional |  |
| actionId | string | optional |  |
| expiringAPIRequestObject | [T](#model-T) | optional |  |
| revertToSkipHealthchecks | boolean | optional |  |


## <a name="model-SingularityHostState"></a> SingularityHostState

| name | type | required | description |
|------|------|----------|-------------|
| hostAddress | string | optional |  |
| hostname | string | optional |  |
| driverStatus | string | optional |  |
| master | boolean | optional |  |
| mesosMaster | string | optional |  |
| uptime | long | optional |  |
| millisSinceLastOffer | long | optional |  |


## <a name="model-SingularityKillTaskRequest"></a> SingularityKillTaskRequest

| name | type | required | description |
|------|------|----------|-------------|
| waitForReplacementTask | boolean | optional | If set to true, treats this task kill as a bounce - launching another task and waiting for it to become healthy |
| override | boolean | optional | If set to true, instructs the executor to attempt to immediately kill the task, rather than waiting gracefully |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


## <a name="model-SingularityLoadBalancerUpdate"></a> SingularityLoadBalancerUpdate

| name | type | required | description |
|------|------|----------|-------------|
| loadBalancerState | [BaragonRequestState](#model-BaragonRequestState) | optional |  Allowable values: UNKNOWN, FAILED, WAITING, SUCCESS, CANCELING, CANCELED, INVALID_REQUEST_NOOP |
| loadBalancerRequestId | [LoadBalancerRequestId](#model-LoadBalancerRequestId) | optional |  |
| uri | string | optional |  |
| method | [LoadBalancerMethod](#model-LoadBalancerMethod) | optional |  Allowable values: PRE_ENQUEUE, ENQUEUE, CHECK_STATE, CANCEL, DELETE |
| message | string | optional |  |
| timestamp | long | optional |  |


## <a name="model-SingularityMachineChangeRequest"></a> SingularityMachineChangeRequest

| name | type | required | description |
|------|------|----------|-------------|
| message | string | optional | A message to show to users about why this action was taken |


## <a name="model-SingularityMachineStateHistoryUpdate"></a> SingularityMachineStateHistoryUpdate

| name | type | required | description |
|------|------|----------|-------------|
| state | [MachineState](#model-MachineState) | optional |  Allowable values: MISSING_ON_STARTUP, ACTIVE, STARTING_DECOMMISSION, DECOMMISSIONING, DECOMMISSIONED, DEAD, FROZEN |
| user | string | optional |  |
| message | string | optional |  |
| timestamp | long | optional |  |
| objectId | string | optional |  |


## <a name="model-SingularityPauseRequest"></a> SingularityPauseRequest

| name | type | required | description |
|------|------|----------|-------------|
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| killTasks | boolean | optional | If set to false, tasks will be allowed to finish instead of killed immediately |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


## <a name="model-SingularityPendingDeploy"></a> SingularityPendingDeploy

| name | type | required | description |
|------|------|----------|-------------|
| currentDeployState | [DeployState](#model-DeployState) | optional |  Allowable values: SUCCEEDED, FAILED_INTERNAL_STATE, CANCELING, WAITING, OVERDUE, FAILED, CANCELED |
| deployProgress | [SingularityDeployProgress](#model-SingularityDeployProgress) | optional |  |
| lastLoadBalancerUpdate | [SingularityLoadBalancerUpdate](#model-SingularityLoadBalancerUpdate) | optional |  |
| deployMarker | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |


## <a name="model-SingularityPendingRequest"></a> SingularityPendingRequest

| name | type | required | description |
|------|------|----------|-------------|
| runId | string | optional |  |
| skipHealthchecks | boolean | optional |  |
| user | string | optional |  |
| requestId | string | optional |  |
| message | string | optional |  |
| timestamp | long | optional |  |
| deployId | string | optional |  |
| actionId | string | optional |  |
| cmdLineArgsList | Array[string] | optional |  |
| pendingType | [PendingType](#model-PendingType) | optional |  Allowable values: IMMEDIATE, ONEOFF, BOUNCE, NEW_DEPLOY, NEXT_DEPLOY_STEP, UNPAUSED, RETRY, UPDATED_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, STARTUP, CANCEL_BOUNCE, TASK_BOUNCE, DEPLOY_CANCELLED |


## <a name="model-SingularityPendingTask"></a> SingularityPendingTask

| name | type | required | description |
|------|------|----------|-------------|
| runId | string | optional |  |
| skipHealthchecks | boolean | optional |  |
| pendingTaskId | [SingularityPendingTaskId](#model-SingularityPendingTaskId) | optional |  |
| user | string | optional |  |
| message | string | optional |  |
| cmdLineArgsList | Array[string] | optional |  |


## <a name="model-SingularityPendingTaskId"></a> SingularityPendingTaskId

| name | type | required | description |
|------|------|----------|-------------|
| nextRunAt | long | optional |  |
| requestId | string | optional |  |
| deployId | string | optional |  |
| pendingType | [PendingType](#model-PendingType) | optional |  Allowable values: IMMEDIATE, ONEOFF, BOUNCE, NEW_DEPLOY, NEXT_DEPLOY_STEP, UNPAUSED, RETRY, UPDATED_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, STARTUP, CANCEL_BOUNCE, TASK_BOUNCE, DEPLOY_CANCELLED |
| instanceNo | int | optional |  |
| createdAt | long | optional |  |
| id | string | optional |  |


## <a name="model-SingularityRack"></a> SingularityRack

| name | type | required | description |
|------|------|----------|-------------|
| currentState | [SingularityMachineStateHistoryUpdate](#model-SingularityMachineStateHistoryUpdate) | optional |  |
| firstSeenAt | long | optional |  |
| id | string | optional |  |


## <a name="model-SingularityRequest"></a> SingularityRequest

| name | type | required | description |
|------|------|----------|-------------|
| readOnlyGroups | [Set](#model-Set) | optional |  |
| schedule | string | optional |  |
| skipHealthchecks | boolean | optional |  |
| waitAtLeastMillisAfterTaskFinishesForReschedule | long | optional |  |
| emailConfigurationOverrides | [Map[SingularityEmailType,List[SingularityEmailDestination]]](#model-Map[SingularityEmailType,List[SingularityEmailDestination]]) | optional |  |
| rackAffinity | Array[string] | optional |  |
| bounceAfterScale | boolean | optional |  |
| slavePlacement | [SlavePlacement](#model-SlavePlacement) | optional |  |
| group | string | optional |  |
| rackSensitive | boolean | optional |  |
| allowedSlaveAttributes | [Map[string,string]](#model-Map[string,string]) | optional |  |
| owners | Array[string] | optional |  |
| requestType | [RequestType](#model-RequestType) | optional |  Allowable values: SERVICE, WORKER, SCHEDULED, ON_DEMAND, RUN_ONCE |
| quartzSchedule | string | optional |  |
| scheduledExpectedRuntimeMillis | long | optional |  |
| requiredSlaveAttributes | [Map[string,string]](#model-Map[string,string]) | optional |  |
| loadBalanced | boolean | optional |  |
| numRetriesOnFailure | int | optional |  |
| killOldNonLongRunningTasksAfterMillis | long | optional |  |
| instances | int | optional |  |
| scheduleType | [ScheduleType](#model-ScheduleType) | optional |  |
| id | string | optional |  |


## <a name="model-SingularityRequestCleanup"></a> SingularityRequestCleanup

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional |  |
| requestId | string | optional |  |
| user | string | optional |  |
| killTasks | boolean | optional |  |
| cleanupType | [RequestCleanupType](#model-RequestCleanupType) | optional |  Allowable values: DELETING, PAUSING, BOUNCE, INCREMENTAL_BOUNCE |
| message | string | optional |  |
| timestamp | long | optional |  |
| deployId | string | optional |  |
| actionId | string | optional |  |


## <a name="model-SingularityRequestDeployState"></a> SingularityRequestDeployState

| name | type | required | description |
|------|------|----------|-------------|
| pendingDeploy | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |
| requestId | string | optional |  |
| activeDeploy | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |


## <a name="model-SingularityRequestHistory"></a> SingularityRequestHistory

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| message | string | optional |  |
| request | [SingularityRequest](#model-SingularityRequest) | optional |  |
| eventType | [RequestHistoryType](#model-RequestHistoryType) | optional |  Allowable values: CREATED, UPDATED, DELETED, PAUSED, UNPAUSED, ENTERED_COOLDOWN, EXITED_COOLDOWN, FINISHED, DEPLOYED_TO_UNPAUSE, BOUNCED, SCALED, SCALE_REVERTED |
| createdAt | long | optional |  |


## <a name="model-SingularityRequestParent"></a> SingularityRequestParent

| name | type | required | description |
|------|------|----------|-------------|
| expiringSkipHealthchecks | [SingularityExpiringSkipHealthchecks](#model-SingularityExpiringSkipHealthchecks) | optional |  |
| state | [RequestState](#model-RequestState) | optional |  Allowable values: ACTIVE, DELETED, PAUSED, SYSTEM_COOLDOWN, FINISHED, DEPLOYING_TO_UNPAUSE |
| pendingDeploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| activeDeploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| expiringPause | [SingularityExpiringPause](#model-SingularityExpiringPause) | optional |  |
| expiringBounce | [SingularityExpiringBounce](#model-SingularityExpiringBounce) | optional |  |
| request | [SingularityRequest](#model-SingularityRequest) | optional |  |
| pendingDeployState | [SingularityPendingDeploy](#model-SingularityPendingDeploy) | optional |  |
| expiringScale | [SingularityExpiringScale](#model-SingularityExpiringScale) | optional |  |
| requestDeployState | [SingularityRequestDeployState](#model-SingularityRequestDeployState) | optional |  |


## <a name="model-SingularityRunNowRequest"></a> SingularityRunNowRequest

| name | type | required | description |
|------|------|----------|-------------|
| runId | string | optional | An id to associate with this request which will be associated with the corresponding launched tasks |
| skipHealthchecks | boolean | optional | If set to true, healthchecks will be skipped for this task run |
| commandLineArgs | Array[string] | optional | Command line arguments to be passed to the task |
| message | string | optional | A message to show to users about why this action was taken |


## <a name="model-SingularitySandbox"></a> SingularitySandbox

| name | type | required | description |
|------|------|----------|-------------|
| slaveHostname | string | optional | Hostname of tasks's slave |
| files | [Array[SingularitySandboxFile]](#model-SingularitySandboxFile) | optional | List of files inside sandbox |
| currentDirectory | string | optional | Current directory |
| fullPathToRoot | string | optional | Full path to the root of the Mesos task sandbox |


## <a name="model-SingularitySandboxFile"></a> SingularitySandboxFile

| name | type | required | description |
|------|------|----------|-------------|
| size | long | optional | File size (in bytes) |
| mode | string | optional | File mode |
| mtime | long | optional | Last modified time |
| name | string | optional | Filename |


## <a name="model-SingularityScaleRequest"></a> SingularityScaleRequest

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | If set to true, healthchecks will be skipped while scaling this request (only) |
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |
| instances | int | optional | The number of instances to scale to |


## <a name="model-SingularityShellCommand"></a> SingularityShellCommand

| name | type | required | description |
|------|------|----------|-------------|
| logfileName | string | optional |  |
| user | string | optional |  |
| options | Array[string] | optional |  |
| name | string | optional |  |


## <a name="model-SingularitySkipHealthchecksRequest"></a> SingularitySkipHealthchecksRequest

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | If set to true, healthchecks will be skipped for all tasks for this request until reversed |
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


## <a name="model-SingularitySlave"></a> SingularitySlave

| name | type | required | description |
|------|------|----------|-------------|
| currentState | [SingularityMachineStateHistoryUpdate](#model-SingularityMachineStateHistoryUpdate) | optional |  |
| attributes | [Map[string,string]](#model-Map[string,string]) | optional |  |
| host | string | optional | Slave hostname |
| rackId | string | optional | Slave rack ID |
| firstSeenAt | long | optional |  |
| id | string | optional |  |


## <a name="model-SingularityState"></a> SingularityState

| name | type | required | description |
|------|------|----------|-------------|
| activeRacks | int | optional |  |
| decomissioningRacks | int | optional |  |
| authDatastoreHealthy | boolean | optional |  |
| activeSlaves | int | optional |  |
| generatedAt | long | optional |  |
| pausedRequests | int | optional |  |
| activeTasks | int | optional |  |
| lbCleanupTasks | int | optional |  |
| overProvisionedRequestIds | Array[string] | optional |  |
| cleaningRequests | int | optional |  |
| deadSlaves | int | optional |  |
| lateTasks | int | optional |  |
| overProvisionedRequests | int | optional |  |
| decommissioningSlaves | int | optional |  |
| unknownRacks | int | optional |  |
| numDeploys | int | optional |  |
| cleaningTasks | int | optional |  |
| unknownSlaves | int | optional |  |
| activeRequests | int | optional |  |
| futureTasks | int | optional |  |
| lbCleanupRequests | int | optional |  |
| decommissioningRacks | int | optional |  |
| finishedRequests | int | optional |  |
| deadRacks | int | optional |  |
| pendingRequests | int | optional |  |
| maxTaskLag | long | optional |  |
| cooldownRequests | int | optional |  |
| hostStates | [Array[SingularityHostState]](#model-SingularityHostState) | optional |  |
| allRequests | int | optional |  |
| underProvisionedRequests | int | optional |  |
| decomissioningSlaves | int | optional |  |
| oldestDeploy | long | optional |  |
| scheduledTasks | int | optional |  |
| underProvisionedRequestIds | Array[string] | optional |  |


## <a name="model-SingularityTask"></a> SingularityTask

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| taskRequest | [SingularityTaskRequest](#model-SingularityTaskRequest) | optional |  |
| offer | [Offer](#model-Offer) | optional |  |
| mesosTask | [TaskInfo](#model-TaskInfo) | optional |  |
| rackId | string | optional |  |


## <a name="model-SingularityTaskCleanup"></a> SingularityTaskCleanup

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| user | string | optional |  |
| cleanupType | [TaskCleanupType](#model-TaskCleanupType) | optional |  Allowable values: USER_REQUESTED, USER_REQUESTED_TASK_BOUNCE, DECOMISSIONING, SCALING_DOWN, BOUNCING, INCREMENTAL_BOUNCE, DEPLOY_FAILED, NEW_DEPLOY_SUCCEEDED, DEPLOY_STEP_FINISHED, DEPLOY_CANCELED, UNHEALTHY_NEW_TASK, OVERDUE_NEW_TASK |
| message | string | optional |  |
| timestamp | long | optional |  |
| actionId | string | optional |  |


## <a name="model-SingularityTaskHealthcheckResult"></a> SingularityTaskHealthcheckResult

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| durationMillis | long | optional |  |
| errorMessage | string | optional |  |
| statusCode | int | optional |  |
| timestamp | long | optional |  |
| responseBody | string | optional |  |


## <a name="model-SingularityTaskHistory"></a> SingularityTaskHistory

| name | type | required | description |
|------|------|----------|-------------|
| directory | string | optional |  |
| task | [SingularityTask](#model-SingularityTask) | optional |  |
| healthcheckResults | [Array[SingularityTaskHealthcheckResult]](#model-SingularityTaskHealthcheckResult) | optional |  |
| loadBalancerUpdates | [Array[SingularityLoadBalancerUpdate]](#model-SingularityLoadBalancerUpdate) | optional |  |
| shellCommandHistory | [Array[SingularityTaskShellCommandHistory]](#model-SingularityTaskShellCommandHistory) | optional |  |
| taskUpdates | [Array[SingularityTaskHistoryUpdate]](#model-SingularityTaskHistoryUpdate) | optional |  |


## <a name="model-SingularityTaskHistoryUpdate"></a> SingularityTaskHistoryUpdate

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| statusReason | string | optional |  |
| statusMessage | string | optional |  |
| taskState | [ExtendedTaskState](#model-ExtendedTaskState) | optional |  Allowable values: TASK_LAUNCHED, TASK_STAGING, TASK_STARTING, TASK_RUNNING, TASK_CLEANING, TASK_FINISHED, TASK_FAILED, TASK_KILLED, TASK_LOST, TASK_LOST_WHILE_DOWN, TASK_ERROR |
| timestamp | long | optional |  |


## <a name="model-SingularityTaskId"></a> SingularityTaskId

| name | type | required | description |
|------|------|----------|-------------|
| requestId | string | optional |  |
| host | string | optional |  |
| deployId | string | optional |  |
| sanitizedHost | string | optional |  |
| rackId | string | optional |  |
| sanitizedRackId | string | optional |  |
| instanceNo | int | optional |  |
| startedAt | long | optional |  |
| id | string | optional |  |


## <a name="model-SingularityTaskIdHistory"></a> SingularityTaskIdHistory

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| runId | string | optional |  |
| updatedAt | long | optional |  |
| lastTaskState | [ExtendedTaskState](#model-ExtendedTaskState) | optional |  |


## <a name="model-SingularityTaskRequest"></a> SingularityTaskRequest

| name | type | required | description |
|------|------|----------|-------------|
| deploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| request | [SingularityRequest](#model-SingularityRequest) | optional |  |
| pendingTask | [SingularityPendingTask](#model-SingularityPendingTask) | optional |  |


## <a name="model-SingularityTaskShellCommandHistory"></a> SingularityTaskShellCommandHistory

| name | type | required | description |
|------|------|----------|-------------|
| shellRequest | [SingularityTaskShellCommandRequest](#model-SingularityTaskShellCommandRequest) | optional |  |
| shellUpdates | [Array[SingularityTaskShellCommandUpdate]](#model-SingularityTaskShellCommandUpdate) | optional |  |


## <a name="model-SingularityTaskShellCommandRequest"></a> SingularityTaskShellCommandRequest

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| user | string | optional |  |
| timestamp | long | optional |  |
| shellCommand | [SingularityShellCommand](#model-SingularityShellCommand) | optional |  |


## <a name="model-SingularityTaskShellCommandRequestId"></a> SingularityTaskShellCommandRequestId

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| name | string | optional |  |
| timestamp | long | optional |  |
| id | string | optional |  |


## <a name="model-SingularityTaskShellCommandUpdate"></a> SingularityTaskShellCommandUpdate

| name | type | required | description |
|------|------|----------|-------------|
| updateType | [UpdateType](#model-UpdateType) | optional |  Allowable values: INVALID, ACKED, STARTED, FINISHED, FAILED |
| outputFilename | string | optional |  |
| message | string | optional |  |
| timestamp | long | optional |  |
| shellRequestId | [SingularityTaskShellCommandRequestId](#model-SingularityTaskShellCommandRequestId) | optional |  |


## <a name="model-SingularityUnpauseRequest"></a> SingularityUnpauseRequest

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | If set to true, instructs new tasks that are scheduled immediately while unpausing to skip healthchecks |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


## <a name="model-SingularityUpdatePendingDeployRequest"></a> SingularityUpdatePendingDeployRequest

| name | type | required | description |
|------|------|----------|-------------|
| requestId | string | optional |  |
| deployId | string | optional |  |
| targetActiveInstances | int | optional |  |


## <a name="model-SingularityVolume"></a> SingularityVolume

| name | type | required | description |
|------|------|----------|-------------|
| hostPath | string | optional |  |
| containerPath | string | optional |  |
| mode | [SingularityDockerVolumeMode](#model-SingularityDockerVolumeMode) | optional |  |


## <a name="model-SingularityWebhook"></a> SingularityWebhook

| name | type | required | description |
|------|------|----------|-------------|
| type | [WebhookType](#model-WebhookType) | optional | Webhook type. Allowable values: TASK, REQUEST, DEPLOY |
| uri | string | optional | URI to POST to. |
| user | string | optional | User that created webhook. |
| timestamp | long | optional |  |
| id | string | optional | Unique ID for webhook. |


## <a name="model-SlaveID"></a> SlaveID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [SlaveID](#model-SlaveID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$SlaveID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$SlaveID&gt;) | optional |  |
| initialized | boolean | optional |  |
| value | string | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-SlaveIDOrBuilder"></a> SlaveIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-TaskID"></a> TaskID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [TaskID](#model-TaskID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskID&gt;) | optional |  |
| initialized | boolean | optional |  |
| value | string | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-TaskIDOrBuilder"></a> TaskIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-TaskInfo"></a> TaskInfo

| name | type | required | description |
|------|------|----------|-------------|
| commandOrBuilder | [CommandInfoOrBuilder](#model-CommandInfoOrBuilder) | optional |  |
| defaultInstanceForType | [TaskInfo](#model-TaskInfo) | optional |  |
| taskIdOrBuilder | [TaskIDOrBuilder](#model-TaskIDOrBuilder) | optional |  |
| taskId | [TaskID](#model-TaskID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskInfo&gt;) | optional |  |
| slaveIdOrBuilder | [SlaveIDOrBuilder](#model-SlaveIDOrBuilder) | optional |  |
| resourcesOrBuilderList | [List[? extends org.apache.mesos.Protos$ResourceOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ResourceOrBuilder]) | optional |  |
| labelsOrBuilder | [LabelsOrBuilder](#model-LabelsOrBuilder) | optional |  |
| data | [ByteString](#model-ByteString) | optional |  |
| executor | [ExecutorInfo](#model-ExecutorInfo) | optional |  |
| containerOrBuilder | [ContainerInfoOrBuilder](#model-ContainerInfoOrBuilder) | optional |  |
| labels | [Labels](#model-Labels) | optional |  |
| executorOrBuilder | [ExecutorInfoOrBuilder](#model-ExecutorInfoOrBuilder) | optional |  |
| container | [ContainerInfo](#model-ContainerInfo) | optional |  |
| healthCheckOrBuilder | [HealthCheckOrBuilder](#model-HealthCheckOrBuilder) | optional |  |
| initialized | boolean | optional |  |
| name | string | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| command | [CommandInfo](#model-CommandInfo) | optional |  |
| healthCheck | [HealthCheck](#model-HealthCheck) | optional |  |
| serializedSize | int | optional |  |
| resourcesList | [List[Resource]](#model-List[Resource]) | optional |  |
| slaveId | [SlaveID](#model-SlaveID) | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| discovery | [DiscoveryInfo](#model-DiscoveryInfo) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| resourcesCount | int | optional |  |
| initializationErrorString | string | optional |  |
| discoveryOrBuilder | [DiscoveryInfoOrBuilder](#model-DiscoveryInfoOrBuilder) | optional |  |


## <a name="model-UnknownFieldSet"></a> UnknownFieldSet

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| serializedSizeAsMessageSet | int | optional |  |
| parserForType | [Parser](#model-Parser) | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |

