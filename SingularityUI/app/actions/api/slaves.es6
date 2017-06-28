import { buildApiAction, buildJsonApiAction } from './base';

export const FetchSlaves = buildApiAction(
  'FETCH_SLAVES',
  {url: '/slaves'}
);

export const FreezeSlave = buildJsonApiAction(
  'FREEZE_SLAVE',
  'POST',
  (slaveId, data) => ({
    url: `/slaves/slave/${slaveId}/freeze`,
    body: data || {}
  })
);

export const DecommissionSlave = buildJsonApiAction(
  'DECOMMISSION_SLAVE',
  'POST',
  (slaveId, data) => ({
    url: `/slaves/slave/${slaveId}/decommission`,
    body: data || {}
  })
);

export const RemoveSlave = buildJsonApiAction(
  'REMOVE_SLAVE',
  'DELETE',
  (slaveId, data) => ({
    url: `/slaves/slave/${slaveId}`,
    body: data || {}
  })
);

export const ReactivateSlave = buildJsonApiAction(
  'ACTIVATE_SLAVE',
  'POST',
  (slaveId, data) => ({
    url: `/slaves/slave/${slaveId}/activate`,
    body: data || {}
  })
);

export const FetchExpiringSlaveStates = buildApiAction(
  'FETCH_EXPIRING_SLAVE_STATES',
  {url: '/slaves/expiring'}
);

export const RemoveExpiringSlaveState = buildJsonApiAction(
  'REMOVE_EXPIRING_SLAVE_STATE',
  'DELETE',
  (slaveId) => ({
    url: `/slaves/slave/${slaveId}/expiring`
  })
);

export const FetchSlaveUsages = buildApiAction(
  'FETCH_SLAVE_USAGES',
  {url : '/usage/slaves'}
);
