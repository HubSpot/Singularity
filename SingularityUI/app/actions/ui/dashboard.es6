import { FetchUserRelevantRequests } from '../../actions/api/requests';

export const refresh = () => (dispatch) => dispatch(FetchUserRelevantRequests.trigger());
