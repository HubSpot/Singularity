import { buildApiAction } from './base';

export const FetchCostData = buildApiAction(
  'FETCH_COSTS',
  (requestId, costsUrlFormat) => {
    const url = costsUrlFormat.replace('{REQUEST_ID}', requestId);
    return ({
      url: url,
      catchStatusCodes: [404]
    })
  },
  (requestId) => requestId
);
