import { buildApiAction, buildJsonApiAction } from './base';

export const FetchDisabledActions = buildApiAction(
  'FETCH_DISABLED_ACTIONS',
  {url: '/disabled-actions'}
);

export const DeleteDisabledAction = buildJsonApiAction(
  'DELETE_DISABLED_ACTION',
  'DELETE',
  (type) => ({
    url: `/disabled-actions/${ type }`
  })
);

export const NewDisabledAction = buildJsonApiAction(
  'NEW_DISABLED_ACTION',
  'POST',
  (type, message) => ({
    url: `/disabled-actions/${ type }`,
    body: message
  })
);
