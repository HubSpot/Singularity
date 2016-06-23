import { buildApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_DEPLOYS', (state = '') => ({url: `deploys/${state}`}));
