const QUARTZ_SCHEDULE_FIELD = {id: 'quartzSchedule', type: 'text', required: true};
const CRON_SCHEDULE_FIELD = {id: 'cronSchedule', type: 'text', required: true};
const INSTANCES_FIELD = {id: 'instances', type: 'number'};
const RACK_SENSITIVE_FIELD = {id: 'rackSensitive', type: 'bool'};
const HIDE_EVEN_NUMBERS_ACROSS_RACKS_HINT_FIELD = {id: 'hideEvenNumberAcrossRacksHint', type: 'bool'};
const RACK_AFFINITY_FIELD = {id: 'rackAffinity', type: 'array'};
const KILL_OLD_NRL_FIELD = {id: 'killOldNonLongRunningTasksAfterMillis', type: 'number'};

export const FIELDS_BY_REQUEST_TYPE = {
  ALL: [
    {id: 'id', type: 'request-id', required: true},
    {id: 'owners', type: 'array', required: true},
    {id: 'requestType', type: 'text', required: true},
    {id: 'slavePlacement', type: 'text'}
  ],
  SERVICE: [
    INSTANCES_FIELD,
    RACK_SENSITIVE_FIELD,
    HIDE_EVEN_NUMBERS_ACROSS_RACKS_HINT_FIELD,
    {id: 'loadBalanced', type: 'bool'},
    RACK_AFFINITY_FIELD
  ],
  WORKER: [
    INSTANCES_FIELD,
    RACK_SENSITIVE_FIELD,
    HIDE_EVEN_NUMBERS_ACROSS_RACKS_HINT_FIELD,
    {id: 'waitAtLeastMillisAfterTaskFinishesForReschedule', type: 'number'},
    RACK_AFFINITY_FIELD
  ],
  SCHEDULED: [
    QUARTZ_SCHEDULE_FIELD,
    CRON_SCHEDULE_FIELD,
    {id: 'scheduleType', type: 'text'},
    {id: 'numRetriesOnFailure', type: 'number'},
    KILL_OLD_NRL_FIELD,
    {id: 'scheduledExpectedRuntimeMillis', type: 'number'}
  ],
  ON_DEMAND: [KILL_OLD_NRL_FIELD],
  RUN_ONCE: [KILL_OLD_NRL_FIELD]
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
