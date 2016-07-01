import { buildApiAction } from './base';

export const FetchWebhooks = buildApiAction(
  'FETCH_WEBHOOKS',
  {url: '/webhooks'}
);
