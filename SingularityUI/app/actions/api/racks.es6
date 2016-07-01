import { buildApiAction } from './base';

export const FetchRacks = buildApiAction(
  'FETCH_RACKS',
  {url: '/racks'}
);

export const FreezeRack = buildApiAction(
  'FREEZE_RACK',
  (slaveId) => ({
    method: 'POST',
    url: `/racks/${slaveId}/freeze`
  })
);

export const DecommissionRack = buildApiAction(
  'DECOMMISSION_RACK',
  (slaveId) => ({
    method: 'POST',
    url: `/racks/${slaveId}/decommission`
  })
);
