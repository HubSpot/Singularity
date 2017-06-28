import { FetchSingularityStatus } from '../../actions/api/state';

export const refresh = () => (dispatch) =>
	dispatch(FetchSingularityStatus.trigger());
