import * as SlavesActions from '../../actions/api/slaves';

const initialState = {
  isFetching: false,
  error: null,
  receivedAt: null,
  data: []
};

export default function slaves(state = initialState, action) {
  switch (action.type) {
    case SlavesActions.FETCH_SLAVES_ERROR:
      return Object.assign({}, state, {
        isFetching: false,
        error: action.error
      });
    case SlavesActions.FETCH_SLAVES_SUCCESS:
      return Object.assign({}, state, {
        isFetching: false,
        error: null,
        receivedAt: Date.now(),
        data: action.data
      });
    case SlavesActions.FETCH_SLAVES_STARTED:
      // Request initiated
      return Object.assign({}, state, {
        isFetching: true,
        error: null
      });
    default:
      return state;
  }
}