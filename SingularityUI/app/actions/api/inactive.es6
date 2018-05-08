import { buildApiAction } from './base';

export const DeactivateHost = buildApiAction(
  'DEACTIVATE_HOST',
  (host) => ({
    method: 'POST',
    url: `/inactive?host=${host}`
  })
);

export const ReactivateHost = buildApiAction(
  'REACTIVATE_HOST',
  (host) => ({
    method: 'DELETE',
    url: `/inactive?host=${host}`
  })
);

export const FetchInactiveHosts = buildApiAction(
  'FETCH_INACTIVE_HOST',
  {url: '/inactive'}
);

export const ClearInactiveHosts = buildApiAction(
  'Clear_INACTIVE_HOSTS',
  {
    method: 'DELETE',
    url: '/inactive/all'
  }
);
