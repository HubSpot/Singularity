import { buildApiAction, buildJsonApiAction } from './base';

export const FetchSlaves = buildApiAction(
  'FETCH_SLAVES',
  {url: '/slaves'}
);

export const FreezeSlave = buildJsonApiAction(
  'FREEZE_SLAVE',
  'POST',
  (slaveId) => ({
    url: `/slaves/slave/${slaveId}/freeze`
  })
);

export const DecommissionSlave = buildJsonApiAction(
  'DECOMMISSION_SLAVE',
  'POST',
  (slaveId) => ({
    url: `/slaves/slave/${slaveId}/decomission`
  })
);
