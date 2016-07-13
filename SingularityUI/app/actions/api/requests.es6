import { buildApiAction, buildJsonApiAction } from './base';

export const FetchRequests = buildApiAction(
  'FETCH_REQUESTS',
  {url: '/requests'}
);

export const FetchRequestsInState = buildApiAction(
  'FETCH_REQUESTS_IN_STATE',
  (state) => {
    if (_.contains(['pending', 'cleanup'], state)) {
      return {url: `/requests/queued/${state}`};
    } else if (_.contains(['all', 'noDeploy', 'activeDeploy'], state)) {
      return {url: '/requests'};
    }
    return {url: `/requests/${state}`};
  }
);

export const FetchRequest = buildApiAction(
  'FETCH_REQUEST',
  (requestId) => ({
    url: `/requests/request/${requestId}`
  }),
  (requestId) => requestId
);

export const SaveRequest = buildJsonApiAction(
  'SAVE_REQUEST',
  'POST',
  (requestData) => ({
    url: '/requests',
    body: requestData
  })
);

export const RemoveRequest = buildJsonApiAction(
  'REMOVE_REQUEST',
  'DELETE',
  (requestId, { message }) => ({
    url: `/requests/request/${requestId}`,
    body: { message }
  })
);

export const RunRequest = buildJsonApiAction(
  'RUN_REQUEST_NOW',
  'POST',
  (requestId, data) => ({
    url: `/requests/request/${requestId}/run`,
    body: data
  })
);

export const FetchRequestRun = buildApiAction(
  'FETCH_REQUEST_RUN',
  (requestId, runId, successResponseCodes = null) => ({
    url: `/requests/request/${ requestId }/run/${runId}`,
    successResponseCodes
  })
);

export const PauseRequest = buildJsonApiAction(
  'PAUSE_REQUEST',
  'POST',
  (requestId, { durationMillis, killTasks, message, actionId }) => ({
    url: `/requests/request/${requestId}/pause`,
    body: { durationMillis, killTasks, message, actionId }
  })
);

export const PersistRequestPause = buildJsonApiAction(
  'PERSIST_REQUEST_PAUSE',
  'DELETE',
  (requestId) => ({
    url: `/requests/request/${requestId}/pause`
  })
);

export const UnpauseRequest = buildJsonApiAction(
  'UNPAUSE_REQUEST',
  'POST',
  (requestId, { skipHealthchecks, message, actionId }) => ({
    url: `/requests/request/${requestId}/unpause`,
    body: { skipHealthchecks, message, actionId }
  })
);

// Remove when Unpause automatically removes the ExpiringPause
export const UnpauseAndPersistRequest = (requestId, data) => {
  return (dispatch) => {
    return dispatch(UnpauseRequest.trigger(requestId, data)).then(
      () => dispatch(PersistRequestPause.trigger(requestId))
    );
  };
};

export const ExitRequestCooldown = buildJsonApiAction(
  'EXIT_REQUEST_COOLDOWN',
  'POST',
  (requestId, {skipHealthchecks, message, actionId}) => ({
    url: `/requests/request/${requestId}/exit-cooldown`,
    body: { skipHealthchecks, message, actionId }
  })
);

export const SkipRequestHealthchecks = buildJsonApiAction(
  'SKIP_REQUEST_HEALTHCHECKS',
  'PUT',
  (requestId, {skipHealthchecks, durationMillis, message, actionId}) => ({
    url: `/requests/request/${requestId}/skipHealthchecks`,
    body: { skipHealthchecks, durationMillis, message, actionId }
  })
);

export const PersistSkipRequestHealthchecks = buildJsonApiAction(
  'PERSIST_SKIP_REQUEST_HEALTHCHECKS',
  'DELETE',
  (requestId) => ({
    url: `/requests/request/${requestId}/skipHealthchecks`
  })
);

export const ScaleRequest = buildJsonApiAction(
  'SCALE_REQUEST',
  'PUT',
  (requestId, {instances, skipHealthchecks, durationMillis, message, actionId}) => ({
    url: `/requests/request/${requestId}/scale`,
    body: { instances, skipHealthchecks, durationMillis, message, actionId }
  })
);

export const PersistRequestScale = buildJsonApiAction(
  'PERSIST_REQUEST_SCALE',
  'DELETE',
  (requestId) => ({
    url: `/requests/request/${requestId}/scale`
  })
);

export const BounceRequest = buildJsonApiAction(
  'BOUNCE_REQUEST',
  'POST',
  (requestId, data) => ({
    url: `/requests/request/${requestId}/bounce`,
    body: data
  })
);

export const CancelRequestBounce = buildJsonApiAction(
  'CANCEL_REQUEST_BOUNCE',
  'DELETE',
  (requestId) => ({
    url: `/requests/request/${requestId}/bounce`
  })
);
