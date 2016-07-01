import { buildApiAction, buildJsonApiAction } from './base';

export const FetchPendingDeploys = buildApiAction(
  'FETCH_PENDING_DEPLOYS',
  {url: '/deploys/pending'}
);

export const SaveDeploy = buildJsonApiAction(
  'SAVE_DEPLOY',
  'POST',
  (deployData) => ({
    url: 'deploys',
    body: deployData
  })
);
