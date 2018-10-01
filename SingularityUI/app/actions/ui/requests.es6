import { FetchRequestsInState, FetchRequestIds } from '../../actions/api/requests';
import { FetchRequestUtilizations } from '../../actions/api/utilization';

export const refresh = (state, propertyFilter) => (dispatch) => {
  const promises = []
  promises.push(dispatch(FetchRequestsInState.trigger(state === 'cleaning' ? 'cleanup' : state, true, propertyFilter)));
  promises.push(dispatch(FetchRequestUtilizations.trigger()));
  return Promise.all(promises);
}

export const initialize = (requestIds) => (dispatch) => {
  if (requestIds.isFetching) {
    return Promise.resolve();
  } else {
    return FetchRequestIds.trigger();
  }
}
