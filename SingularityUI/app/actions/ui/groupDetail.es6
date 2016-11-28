import { FetchGroups } from '../../actions/api/requestGroups';

export const refresh = () => (dispatch) =>
  dispatch(FetchGroups.trigger());
