import { FetchRequestsInState } from '../../actions/api/requests';

export const refresh = (state) => (dispatch) =>
	dispatch(FetchRequestsInState.trigger(state === 'cleaning' ? 'cleanup' : state, true))