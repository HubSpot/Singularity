export const FIELDS = {
  all: [
    {id: 'id', type: 'text', required: true},
    {id: 'executorType', type: 'text', default: 'default', required: true},
    {id: 'env', type: 'map'},
    {
      id: 'containerInfo',
      type: 'object',
      values: [{id: 'type', type: 'text', default: 'MESOS', required: true}]
    },
    {
      id: 'resources',
      type: 'object',
      values: [
        {id: 'cpus', type: 'number', default: 1},
        {id: 'memoryMb', type: 'number', default: 128},
        {id: 'numPorts', type: 'number', default: 0},
        {id: 'diskMb', type: 'number'}
      ]
    }
  ],
  defaultExecutor: [
    {id: 'command', type: 'text'},
    {id: 'uris', type: 'mesosArtifacts'},
    {id: 'arguments', type: 'array', arrayType: 'text'}
  ],
  customExecutor: [
    {id: 'customExecutorCmd', type: 'text'},
    {
      id: 'executorData',
      type: 'object',
      values: [
        {id: 'cmd', type: 'text'},
        {id: 'extraCmdLineArgs', type: 'array', arrayType: 'text'},
        {id: 'user', type: 'text', default: 'root'},
        {id: 'sigKillProcessesAfterMillis', type: 'number', default: 120000},
        {id: 'successfulExitCodes', type: 'array', arrayType: 'number'},
        {id: 'maxTaskThreads', type: 'number'},
        {id: 'loggingTag', type: 'text'},
        {id: 'loggingExtraFields', type: 'map'},
        {id: 'logrotateFrequency', type: 'text'},
        {id: 'preserveTaskSandboxAfterFinish', type: 'text'},
        {id: 'skipLogrotateAndCompress', type: 'text'},
        {id: 'loggingS3Bucket', type: 'text'},
        {id: 'maxOpenFiles', type: 'number'},
        {id: 'runningSentinel', type: 'text'},
        {id: 'embeddedArtifacts', type: 'artifacts'},
        {id: 'externalArtifacts', type: 'artifacts'},
        {id: 's3Artifacts', type: 'artifacts'}
      ]
    }
  ],
  dockerContainer: [
    {
      id: 'containerInfo',
      type: 'object',
      values: [
        {
          id: 'docker',
          type: 'object',
          values: [
            {id: 'image', type: 'text', required: true},
            {id: 'network', type: 'text', default: 'NONE'},
            {id: 'dockerParameters', type: 'dockerParameters'},
            {id: 'privileged', type: 'text'},
            {id: 'forcePullImage', type: 'text'},
            {id: 'volumes', type: 'volumes'},
            {id: 'portMappings', type: 'portMappings'}
          ]
        }
      ]
    },
  ],
  loadBalancer: [
    {id: 'serviceBasePath', type: 'text', required: true},
    {id: 'loadBalancerGroups', type: 'array', arrayType: 'text', required: true},
    {id: 'loadBalancerOptions', type: 'map'},
    {id: 'loadBalancerPortIndex', type: 'number', default: 0}
  ],
  healthChecker: [
    {
      id: 'healthcheck',
      type: 'object',
      values: [
        {id: 'uri', type: 'text'},
        {id: 'portIndex', type: 'number'},
        {id: 'portNumber', type: 'number'},
        {id: 'protocol', type: 'text', default: 'HTTP'},
        {id: 'startupDelaySeconds', type: 'number'},
        {id: 'startupTimeoutSeconds', type: 'number'},
        {id: 'startupIntervalSeconds', type: 'number'},
        {id: 'responseTimeoutSeconds', type: 'number'},
        {id: 'intervalSeconds', type: 'number'},
        {id: 'maxRetries', type: 'number'},
        {id: 'failureStatusCodes', type: 'array', arrayType: 'number', required: false},
      ]
    },
    {id: 'deployHealthTimeoutSeconds', type: 'number'},
    {id: 'skipHealthchecksOnDeploy', type: 'text'},
    {id: 'considerHealthyAfterRunningForSeconds', type: 'number'}
  ]
};

export const MESOS_ARTIFACT_FIELDS = [
  {id: 'uri', type: 'text', required: true},
  {id: 'cache', type: 'text'},
  {id: 'executable', type: 'text'},
  {id: 'extract', type: 'text', default: true}
];

export const ARTIFACT_FIELDS = {
  all: [
    {id: 'name', type: 'text', required: true},
    {id: 'type', type: 'text', required: true},
    {id: 'filename', type: 'text', required: true},
    {id: 'md5Sum', type: 'text'}
  ],
  embedded: [
    {id: 'content', type: 'base64'}
  ],
  external: [
    {id: 'url', type: 'text', required: true},
    {id: 'filesize', type: 'number'}
  ],
  s3: [
    {id: 's3Bucket', type: 'text', required: true},
    {id: 's3ObjectKey', type: 'text', required: true},
    {id: 'filesize', type: 'number'}
  ]
};

export const DOCKER_PORT_MAPPING_FIELDS = [
  {id: 'containerPortType', type: 'text', default: 'LITERAL', required: true},
  {id: 'containerPort', type: 'text', required: true},
  {id: 'hostPortType', type: 'text', default: 'LITERAL', required: true},
  {id: 'hostPort', type: 'text', required: true},
  {id: 'protocol', type: 'text', default: 'tcp'}
];

export const DOCKER_PARAMETERS_FIELDS = [
  {id: 'key', type: 'text', required: true},
  {id: 'value', type: 'text'},
];

export const DOCKER_VOLUME_FIELDS = [
  {id: 'containerPath', type: 'text', required: true},
  {id: 'hostPath', type: 'text', required: true},
  {id: 'mode', type: 'text', default: 'RO', required: true}
];

function makeIndexedFields(fields) {
  const indexedFields = {};
  for (const field of fields) {
    if (field.type === 'object') {
      _.extend(indexedFields, makeIndexedFields(field.values));
    } else {
      indexedFields[field.id] = field;
    }
  }
  return indexedFields;
}

export const INDEXED_FIELDS = _.extend(
  {},
  makeIndexedFields(FIELDS.all),
  makeIndexedFields(FIELDS.customExecutor),
  makeIndexedFields(FIELDS.defaultExecutor),
  makeIndexedFields(FIELDS.dockerContainer),
  makeIndexedFields(FIELDS.loadBalancer),
  makeIndexedFields(FIELDS.healthChecker)
);
export const INDEXED_ARTIFACT_FIELDS = _.extend(
  {},
  makeIndexedFields(ARTIFACT_FIELDS.all),
  makeIndexedFields(ARTIFACT_FIELDS.embedded),
  makeIndexedFields(ARTIFACT_FIELDS.external),
  makeIndexedFields(ARTIFACT_FIELDS.s3)
);
export const INDEXED_DOCKER_PORT_MAPPING_FIELDS = makeIndexedFields(DOCKER_PORT_MAPPING_FIELDS);
export const INDEXED_DOCKER_PARAMETERS_FIELDS = makeIndexedFields(DOCKER_PARAMETERS_FIELDS);
export const INDEXED_DOCKER_VOLUME_FIELDS = makeIndexedFields(DOCKER_VOLUME_FIELDS);
export const INDEXED_ALL_FIELDS = makeIndexedFields(FIELDS.all);
export const INDEXED_CUSTOM_EXECUTOR_FIELDS = makeIndexedFields(FIELDS.customExecutor);
export const INDEXED_DEFAULT_EXECUTOR_FIELDS = makeIndexedFields(FIELDS.defaultExecutor);
export const INDEXED_DOCKER_CONTAINER_FIELDS = makeIndexedFields(FIELDS.dockerContainer);
export const INDEXED_LOAD_BALANCER_FIELDS = makeIndexedFields(FIELDS.loadBalancer);
export const INDEXED_HEALTH_CHECKER_FIELDS = makeIndexedFields(FIELDS.healthChecker);
export const INDEXED_MESOS_ARTIFACT_FIELDS = makeIndexedFields(MESOS_ARTIFACT_FIELDS)
export const INDEXED_ALL_ARTIFACT_FIELDS = makeIndexedFields(ARTIFACT_FIELDS.all);
export const INDEXED_EMBEDDED_ARTIFACT_FIELDS = makeIndexedFields(ARTIFACT_FIELDS.embedded);
export const INDEXED_EXTERNAL_ARTIFACT_FIELDS = makeIndexedFields(ARTIFACT_FIELDS.external);
export const INDEXED_S3_ARTIFACT_FIELDS = makeIndexedFields(ARTIFACT_FIELDS.s3);
