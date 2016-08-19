import { buildApiAction, buildJsonApiAction } from './base';

export const FetchSlaves = buildApiAction(
  'FETCH_SLAVES',
  {url: '/slaves'}
);

export const FreezeSlave = buildJsonApiAction(
  'FREEZE_SLAVE',
  'POST',
  (slaveId, message) => ({
    url: `/slaves/slave/${slaveId}/freeze`,
    body: { message }
  })
);

export const DecommissionSlave = buildJsonApiAction(
  'DECOMMISSION_SLAVE',
  'POST',
  (slaveId, message) => ({
    url: `/slaves/slave/${slaveId}/decommission`,
    body: { message }
  })
);

export const RemoveSlave = buildJsonApiAction(
  'REMOVE_SLAVE',
  'DELETE',
  (slaveId, message) => ({
    url: `/slaves/slave/${slaveId}`,
    body: { message }
  })
);

export const ReactivateSlave = buildJsonApiAction(
  'ACTIVATE_SLAVE',
  'POST',
  (slaveId, message) => ({
    url: `/slaves/slave/${slaveId}/activate`,
    body: { message }
  })
);
