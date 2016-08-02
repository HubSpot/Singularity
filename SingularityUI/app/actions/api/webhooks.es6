import { buildApiAction, buildJsonApiAction } from './base';

export const FetchWebhooks = buildApiAction(
  'FETCH_WEBHOOKS',
  {url: '/webhooks/summary'}
);

export const DeleteWebhook = buildJsonApiAction(
  'DELETE_WEBHOOK',
  'DELETE',
  (webhookId) => ({
    url: `/webhooks/?webhookId=${ webhookId }`
  })
);

export const NewWebhook = buildJsonApiAction(
  'NEW_WEBHOOK',
  'POST',
  (uri, type, user) => ({
    url: '/webhooks',
    body: { uri, type, user }
  })
);
