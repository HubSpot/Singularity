import { buildApiAction, buildJsonApiAction } from './base';

export const FetchDisastersData = buildApiAction(
  'FETCH_DISASTERS_DATA',
  {url: '/disasters/stats'}
);

export const DeleteDisaster = buildJsonApiAction(
  'DELETE_DISASTER',
  'DELETE',
  (type) => ({
    url: `/disasters/active/${ type }`
  })
);

export const NewDisaster = buildJsonApiAction(
  'NEW_DISASTER',
  'POST',
  (type) => ({
    url: `/disasters/active/${ type }`
  })
);

export const DisableAutomatedActions = buildJsonApiAction(
  'DISABLE_AUTOMATED_ACTIONS',
  'POST',
  (type) => ({
    url: '/disasters/disable'
  })
);

export const EnableAutomatedActions = buildJsonApiAction(
  'ENABLE_AUTOMATED_ACTIONS',
  'POST',
  (type) => ({
    url: '/disasters/enable'
  })
);

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
