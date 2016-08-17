import { buildApiAction } from './base';

export const FetchUser = buildApiAction(
  'FETCH_USER',
  {url: '/auth/user'}
);
