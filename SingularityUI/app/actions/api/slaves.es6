import { buildApiAction, buildJsonApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_SLAVES', {url: '/slaves'});
export const FreezeAction = buildJsonApiAction('FREEZE_SLAVE', 'POST', (slaveId) => _.extend({}, {url: `/slaves/slave/${slaveId}/freeze`}));
export const DecommissionAction = buildJsonApiAction('DECOMMISSION_SLAVE', 'POST', (slaveId) => _.extend({}, {url: `/slaves/slave/${slaveId}/decomission`}));
