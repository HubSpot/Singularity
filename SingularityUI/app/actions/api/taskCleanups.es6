import { buildApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_TASK_CLEANUPS', {url: '/tasks/cleaning'});
