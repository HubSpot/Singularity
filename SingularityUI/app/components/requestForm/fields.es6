import Utils from '../../utils';

const QUARTZ_SCHEDULE_FIELD = {id: 'quartzSchedule', type: 'string', required: true};
const CRON_SCHEDULE_FIELD = {id: 'cronSchedule', type: 'string', required: true};
const INSTANCES_FIELD = {id: 'instances', type: 'number'};
const RACK_SENSITIVE_FIELD = {id: 'rackSensitive', type: 'bool'};
const HIDE_EVEN_NUMBERS_ACROSS_RACKS_HINT_FIELD = {id: 'hideEvenNumberAcrossRacksHint', type: 'bool'};
const EXECUTION_TIME_LIMIT_FIELD = {id: 'taskExecutionTimeLimitMillis', type: 'number'}
const RACK_AFFINITY_FIELD = {
  id: 'rackAffinity',
  type: {
    typeName: 'array',
    arrayType: 'string'
  }
};
const KILL_OLD_NRL_FIELD = {id: 'killOldNonLongRunningTasksAfterMillis', type: 'number'};
const BOUNCE_AFTER_SCALE_FIELD = {id: 'bounceAfterScale', type: 'bool'};

export const FIELDS_BY_REQUEST_TYPE = {
  ALL: [
    {id: 'id', type: 'request-id', required: true},
    {
      id: 'owners',
      type: {
        typeName: 'array',
        arrayType: 'string'
      }
    },
    {id: 'requestType', type: { typeName: 'enum', enumType: Utils.enums.SingularityRequestTypes}, required: true},
    {id: 'slavePlacement', type: 'string'},
    {
      id: 'requiredSlaveAttributes',
      type: {
        typeName: 'map',
        mapFrom: 'string',
        mapTo: 'string'
      }
    },
    {
      id: 'allowedSlaveAttributes',
      type: {
        typeName: 'map',
        mapFrom: 'string',
        mapTo: 'string'
      }
    },
    {id: 'group', type: 'string'},
    {id: 'maxTasksPerOffer', type: 'number'},
    {id: 'taskLogErrorRegex', type: 'string'},
    {id: 'taskLogErrorRegexCaseSensitive', type: 'bool'},
    {
      id: 'readOnlyGroups',
      type: {
        typeName: 'array',
        arrayType: 'string'
      }
    },
    {
      id: 'readWriteGroups',
      type: {
        typeName: 'array',
        arrayType: 'string'
      }
    },
    {id: 'skipHealthchecks', type: 'bool'},
    {
      id: 'emailConfigurationOverrides',
      type: {
        typeName: 'map',
        mapFrom: {
          typeName: 'enum',
          enumType: Utils.enums.SingularityEmailType
        },
        mapTo: {
          typeName: 'array',
          arrayType: {
            typeName: 'enum',
            enumType: Utils.enums.SingularityEmailDestination
          }
        }
      }
    }
  ],
  SERVICE: [
    INSTANCES_FIELD,
    RACK_SENSITIVE_FIELD,
    HIDE_EVEN_NUMBERS_ACROSS_RACKS_HINT_FIELD,
    {id: 'loadBalanced', type: 'bool'},
    {id: 'allowBounceToSameHost', type: 'bool'},
    RACK_AFFINITY_FIELD,
    BOUNCE_AFTER_SCALE_FIELD
  ],
  WORKER: [
    INSTANCES_FIELD,
    RACK_SENSITIVE_FIELD,
    HIDE_EVEN_NUMBERS_ACROSS_RACKS_HINT_FIELD,
    {id: 'waitAtLeastMillisAfterTaskFinishesForReschedule', type: 'number'},
    {id: 'allowBounceToSameHost', type: 'bool'},
    RACK_AFFINITY_FIELD,
    BOUNCE_AFTER_SCALE_FIELD
  ],
  SCHEDULED: [
    QUARTZ_SCHEDULE_FIELD,
    CRON_SCHEDULE_FIELD,
    {id: 'scheduleTimeZone', type: 'string'},
    {id: 'scheduleType', type: 'string'},
    {id: 'numRetriesOnFailure', type: 'number'},
    KILL_OLD_NRL_FIELD,
    {id: 'scheduledExpectedRuntimeMillis', type: 'number'},
    EXECUTION_TIME_LIMIT_FIELD
  ],
  ON_DEMAND: [INSTANCES_FIELD, KILL_OLD_NRL_FIELD, EXECUTION_TIME_LIMIT_FIELD],
  RUN_ONCE: [KILL_OLD_NRL_FIELD, EXECUTION_TIME_LIMIT_FIELD]
};

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
  makeIndexedFields(FIELDS_BY_REQUEST_TYPE.ALL),
  makeIndexedFields(FIELDS_BY_REQUEST_TYPE.SERVICE),
  makeIndexedFields(FIELDS_BY_REQUEST_TYPE.WORKER),
  makeIndexedFields(FIELDS_BY_REQUEST_TYPE.SCHEDULED),
  makeIndexedFields(FIELDS_BY_REQUEST_TYPE.ON_DEMAND),
  makeIndexedFields(FIELDS_BY_REQUEST_TYPE.RUN_ONCE)
);
