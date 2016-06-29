import { buildApiAction, buildJsonApiAction } from './base';

export const FetchAction = buildApiAction(
  'FETCH_REQUEST',
  (requestId) => ({url: `/requests/request/${ requestId }`}),
  (requestId) => requestId
);

export const SaveAction = buildJsonApiAction('SAVE_REQUEST', 'POST', (requestData) => ({url: '/requests', body: requestData}));

export const UnpauseAction = buildJsonApiAction(
  'UNPAUSE_REQUEST',
  'POST',
  (requestId, message) => ({
    url: `/requests/request/${requestId}/unpause`,
    body: { message }
  })
);

export const RemoveAction = buildJsonApiAction(
  'REMOVE_REQUEST',
  'DELETE',
  (requestId, message) => ({
    url: `/requests/request/${requestId}`,
    body: { message }
  })
);
