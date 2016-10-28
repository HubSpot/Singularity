import { buildApiAction, buildJsonApiAction } from './base';

export const FetchUserSettings = buildApiAction(
  'FETCH_USER_SETTINGS',
  {
    url: `/users/settings`,
    catchStatusCodes: [404] // Remove this once the API doesn't 404 when user has nothing set
  }
);

export const UpdateUserSettings = buildJsonApiAction(
  'UPDATE_USER_SETTINGS',
  'POST',
  (settings) => ({
    url: `/users/settings`,
    body: settings
  })
);

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