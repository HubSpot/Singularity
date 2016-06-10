import fetch from 'isomorphic-fetch';

import buildApiAction from './base';

export const FetchAction = buildApiAction('FETCH_RACKS', '/racks');
export const FreezeAction = buildApiAction('FREEZE_RACK', (slaveId) => { "/racks/`${slaveId}`/freeze"; }, {method: 'POST'});
export const DecommissionAction = buildApiAction('DECOMMISSION_RACK', (slaveId) => { "/racks/`${slaveId}`/decommission"; }, {method: 'POST'});