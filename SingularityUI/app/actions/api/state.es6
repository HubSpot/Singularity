import { buildApiAction } from './base';

export const FetchSingularityStatus = buildApiAction(
  'FETCH_SINGULARITY_STATUS',
  {url: '/state'}
);
