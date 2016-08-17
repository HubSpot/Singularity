import { buildApiAction, buildJsonApiAction } from './base';

export const FetchUser = buildApiAction(
  'FETCH_USER',
  {url: '/auth/user'}
);

export const FetchUserSettings = buildApiAction(
  'FETCH_USER_SETTINGS',
  userId => ({
    url: `/users/settings?userId=${userId}`,
    catchStatusCodes: [404] // Remove this once the API doesn't 404 when user has nothing set
  })
);

export const UpdateUserSettings = buildJsonApiAction(
  'UPDATE_USER_SETTINGS',
  'POST',
  (userId, settings) => ({
    url: `/users/settings?userId=${userId}`,
    body: settings
  })
);

export const AddStarredRequests = buildJsonApiAction(
  'ADD_STARRED_REQUESTS',
  'POST',
  (userId, starredRequestIds) => ({
    url: '/users/settings/starred-requests',
    body: { userId, starredRequestIds }
  })
);

export const DeleteStarredRequests = buildJsonApiAction(
  'DELETE_STARRED_REQUESTS',
  'DELETE',
  (userId, starredRequestIds) => ({
    url: '/users/settings/starred-requests',
    body: { userId, starredRequestIds }
  })
);
