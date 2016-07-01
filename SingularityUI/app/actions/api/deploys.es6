import { buildApiAction } from './base';

export const FetchPendingDeploys = buildApiAction(
  'FETCH_PENDING_DEPLOYS',
  {url: '/deploys/pending'}
);
