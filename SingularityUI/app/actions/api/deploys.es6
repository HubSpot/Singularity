import { buildApiAction, buildJsonApiAction } from './base';

export const FetchPendingDeploys = buildApiAction(
  'FETCH_PENDING_DEPLOYS',
  {url: '/deploys/pending'}
);

export const SaveDeploy = buildJsonApiAction(
  'SAVE_DEPLOY',
  'POST',
  (deployData) => ({
    url: '/deploys',
    body: deployData,
    catchStatusCodes: [400]
  })
);

export const AdvanceDeploy = buildJsonApiAction(
  'ADVANCE_DEPLOY',
  'POST',
  (deployId, requestId, targetActiveInstances) => ({
    url: '/deploys/update',
    body: { deployId, requestId, targetActiveInstances }
  })
);

export const CancelDeploy = buildJsonApiAction(
  'CANCEL_DEPLOY',
  'DELETE',
  (deployId, requestId) => ({
    url: `/deploys/deploy/${deployId}/request/${requestId}`
  })
);
