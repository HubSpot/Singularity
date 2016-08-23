import { buildApiAction, buildJsonApiAction } from './base';

export const FetchDisabledActions = buildApiAction(
  'FETCH_DISABLED_ACTIONS',
  {url: '/disasters/disabled-actions'}
);

export const DeleteDisabledAction = buildJsonApiAction(
  'DELETE_DISABLED_ACTION',
  'DELETE',
  (type) => ({
    url: `/disasters/disabled-actions/${ type }`
  })
);

export const NewDisabledAction = buildJsonApiAction(
  'NEW_DISABLED_ACTION',
  'POST',
  (type, message) => ({
    url: `/disasters/disabled-actions/${ type }`,
    body: message
  })
);
