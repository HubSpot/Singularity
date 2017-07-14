import { buildApiAction } from './base';

export const FetchUtilization = buildApiAction(
  'FETCH_UTILIZATION',
  {url: '/usage/cluster/utilization'}
);

