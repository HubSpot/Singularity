import { buildApiAction, buildJsonApiAction } from './base';

export const FetchRequests = buildApiAction(
  'FETCH_REQUESTS',
  {url: '/requests'}
);

export const FetchRequest = buildApiAction(
  'FETCH_REQUEST',
  (requestId) => ({
    url: `/requests/request/${requestId}`
  }),
  (requestId) => requestId
);

export const SaveRequest = buildJsonApiAction(
  'SAVE_REQUEST',
  'POST',
  (requestData) => ({
    url: '/requests',
    body: requestData
  })
);

export const RemoveRequest = buildJsonApiAction(
  'REMOVE_REQUEST',
  'DELETE',
  (requestId, message) => ({
    url: `/requests/request/${requestId}`,
    body: { message }
  })
);

export const RunRequest = buildJsonApiAction(
  'RUN_REQUEST_NOW',
  'POST',
  (requestId, data) => ({
    url: `/requests/request/${requestId}/run`,
    body: { data }
  })
);

export const FetchRequestRun = buildApiAction(
  'FETCH_REQUEST_RUN',
  (requestId, runId) => ({
    url: `/requests/request/${ requestId }/run/${runId}`
  })
);

export const UnpauseRequest = buildJsonApiAction(
  'UNPAUSE_REQUEST',
  'POST',
  (requestId, message) => ({
    url: `/requests/request/${requestId}/unpause`,
    body: { message }
  })
);
