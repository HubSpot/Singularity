import fetch from 'isomorphic-fetch';

import buildApiAction from './base';

const POST_JSON = {method: 'POST', headers: {'Accept': 'application/json', 'Content-Type': 'application/json'}};

export const FetchAction = buildApiAction('FETCH_SLAVES', '/slaves');
export const FreezeAction = buildApiAction('FREEZE_SLAVE', (slaveId) => `/slaves/slave/${slaveId}/freeze`, POST_JSON);
export const DecommissionAction = buildApiAction('DECOMMISSION_SLAVE', (slaveId) => { `/slaves/slave/${slaveId}/decommission`; }, POST_JSON);
