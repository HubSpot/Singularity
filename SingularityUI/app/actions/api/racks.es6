import { buildApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_RACKS', {url: '/racks'});
export const FreezeAction = buildApiAction('FREEZE_RACK', (slaveId) => {return {method: 'POST', url: `/racks/${slaveId}/freeze`}});
export const DecommissionAction = buildApiAction('DECOMMISSION_RACK', (slaveId) => {return {method: 'POST', url: `/racks/${slaveId}/decommission`}});
