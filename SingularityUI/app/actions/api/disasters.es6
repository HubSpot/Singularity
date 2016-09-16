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
    body: message == null ? {type: type} : {message: message, type: type}
  })
);

export const FetchPriorityFreeze = buildApiAction(
  'FETCH_PRIORITY_FREEZE',
  (catchStatusCodes) => ({
    url: '/priority/freeze',
    catchStatusCodes
  })
);

export const DeletePriorityFreeze = buildJsonApiAction(
  'DELETE_PRIORITY_FREEZE',
  'DELETE',
  {url: '/priority/freeze'}
);

export const NewPriorityFreeze = buildJsonApiAction(
  'NEW_PRIORITY_FREEZE',
  'POST',
  (minPriority, killTasks, message) => ({
    url: '/priority/freeze',
    body: message == null ? {minimumPriorityLevel: minPriority, killTasks: killTasks} : {minimumPriorityLevel: minPriority, killTasks: killTasks, message: message}
  })
);
