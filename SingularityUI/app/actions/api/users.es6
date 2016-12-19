import { buildApiAction, buildJsonApiAction } from './base';

export const AddStarredRequests = buildJsonApiAction(
  'ADD_STARRED_REQUESTS',
  'POST',
  (starredRequestIds) => ({
    url: '/users/settings/starred-requests',
    body: { starredRequestIds }
  })
);

export const DeleteStarredRequests = buildJsonApiAction(
  'DELETE_STARRED_REQUESTS',
  'DELETE',
  (starredRequestIds) => ({
    url: '/users/settings/starred-requests',
    body: { starredRequestIds }
  })
);