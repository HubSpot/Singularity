import { buildApiAction } from './base';

export const FetchUtilization = buildApiAction(
  'FETCH_UTILIZATION',
  (catchStatusCodes = null) => ({
    url: '/usage/cluster/utilization',
    catchStatusCodes
  })
);

export const FetchRequestUtilizations = buildApiAction(
  'FETCH_REQUEST_UTILIZATIONS',
  (catchStatusCodes = null) => ({
    url: '/usage/requests',
    catchStatusCodes
  })
);


export const FetchRequestUtilization = buildApiAction(
  'FETCH_REQUEST_UTILIZATION',
  (requestId, catchStatusCodes = null) => ({
    url: `/usage/requests/request/${requestId}`,
    catchStatusCodes
  }),
  (requestId) => requestId
);
