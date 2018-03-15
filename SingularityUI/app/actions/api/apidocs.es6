import { buildApiAction } from './base';

export const FetchOpenApiJson = buildApiAction(
  'FETCH_OPEN_API_JSON',
  {url: '/openapi.json'}
);
