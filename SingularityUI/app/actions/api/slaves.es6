import buildApiAction from './base';

const POST_JSON = {method: 'POST', headers: {'Accept': 'application/json', 'Content-Type': 'application/json'}};

export const FetchAction = buildApiAction('FETCH_SLAVES', {url: '/slaves'});
export const FreezeAction = buildApiAction('FREEZE_SLAVE', (slaveId) => _.extend({}, POST_JSON, {url: `/slaves/slave/${slaveId}/freeze`}));
export const DecommissionAction = buildApiAction('DECOMMISSION_SLAVE', (slaveId) => _.extend({}, POST_JSON, {url: `/slaves/slave/${slaveId}/decomission`}));
