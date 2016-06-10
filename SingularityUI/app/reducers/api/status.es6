import * as StatusActions from '../../actions/api/status';

const initialState = {
  isFetching: false,
  error: null,
  receivedAt: null,
  data: {}
};

export default function status(state = initialState, action) {
  switch (action.type) {
    case StatusActions.FETCH_STATUS_ERROR:
      return Object.assign({}, state, {
        isFetching: false,
        error: action.error
      });
    case StatusActions.FETCH_STATUS_SUCCESS:
      return Object.assign({}, state, {
        isFetching: false,
        error: null,
        receivedAt: Date.now(),
        data: action.data
      });
    case StatusActions.FETCH_STATUS_STARTED:
      // Request initiated
      return Object.assign({}, state, {
        isFetching: true,
        error: null
      });
    default:
      return state;
  }
}
