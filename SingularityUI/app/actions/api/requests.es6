import { buildApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_REQUESTS', (state) => {
  if (_.contains(['pending', 'cleanup'], state)) {
    return {url: `/requests/queued/${state}`}
  } else if (state === 'all') {
    return {url: '/requests'}
  } else {
    return {url: `/requests/${state}`}
  }
});
