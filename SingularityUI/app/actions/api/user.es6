import { buildApiAction } from './base';

export const FetchUser = buildApiAction(
  'FETCH_USER',
  {url: '/auth/user'}
);

export const FetchUserSettings = buildApiAction(
  'FETCH_USER_SETTINGS',
  id => ({
    url: `/users/settings?userId=${id}`,
    catchStatusCodes: [404] // Remove this once the API doesn't 404 when user has nothing set
  })
);

export const UpdateUserSettings = buildApiAction(
  'FETCH_USER_SETTINGS',
  (id, settings) => ({
    url: '/users/settings',
    body: { id, settings }
  })
);
