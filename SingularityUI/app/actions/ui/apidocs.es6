import { FetchOpenApiJson } from '../../actions/api/apidocs';

export const refresh = () => (dispatch) =>
  dispatch(FetchOpenApiJson.trigger());