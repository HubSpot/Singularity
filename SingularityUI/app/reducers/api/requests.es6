import * as RequestsActions from '../../actions/api/requests';

const initialState = {
  isFetching: false,
  error: null,
  receivedAt: null,
  data: []
};

const requests = (state = initialState, action) => {
  switch (action.type) {
    case RequestsActions.FETCH_REQUESTS_ERROR:
      return Object.assign({}, state, {
        isFetching: false,
        error: action.error
      });
    case RequestsActions.FETCH_REQUESTS_SUCCESS:
      return Object.assign({}, state, {
        isFetching: false,
        error: null,
        receivedAt: Date.now(),
        data: action.data
      });
    case RequestsActions.FETCH_REQUESTS_STARTED:
      // Request initiated
      return Object.assign({}, state, {
        isFetching: true,
        error: null
      });
    default:
      return state;
  }
};

export default requests;
