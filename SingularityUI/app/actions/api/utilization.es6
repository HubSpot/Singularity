import { buildApiAction } from './base';

export const FetchUtilization = buildApiAction(
  'FETCH_UTILIZATION',
  (catchStatusCodes = null) => ({
    url: '/usage/cluster/utilization',
    catchStatusCodes
  })
);

