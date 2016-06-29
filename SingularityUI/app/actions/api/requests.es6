import { buildApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_REQUESTS', (state) => {
  if (_.contains(['pending', 'cleanup'], state)) {
    return {url: `/requests/queued/${state}`}
  } else if (_.contains(['all', 'noDeploy', 'activeDeploy'], state)) {
    return {url: '/requests'}
  } else {
    return {url: `/requests/${state}`}
  }
});
