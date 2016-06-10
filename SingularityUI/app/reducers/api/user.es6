import * as UserActions from '../../actions/api/user';

const initialState = {
  isFetching: false,
  error: null,
  receivedAt: null,
  data: {}
};

export default function user(state = initialState, action) {
  switch (action.type) {
    case UserActions.FETCH_USER_ERROR:
      return Object.assign({}, state, {
        isFetching: false,
        error: action.error
      });
    case UserActions.FETCH_USER_SUCCESS:
      return Object.assign({}, state, {
        isFetching: false,
        error: null,
        receivedAt: Date.now(),
        data: action.data
      });
    case UserActions.FETCH_USER_STARTED:
      // Request initiated
      return Object.assign({}, state, {
        isFetching: true,
        error: null
      });
    default:
      return state;
  }
}
