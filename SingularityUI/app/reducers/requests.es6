import { FETCH_REQUESTS } from '../actions/requests';

const initialState = {
  search: {
    text: null,
    state: null,
    type: null
  },
  isFetching: false,
  error: null,
  receivedAt: null,
  all: []
};

export default function requests(state = initialState, action) {
  switch (action.type) {
    case FETCH_REQUESTS:
      if (action.status === 'error') {
        return Object.assign({}, state, {
          isFetching: false,
          error: action.error
        });
      }
      else if (action.status === 'success') {
        return Object.assign({}, state, {
          isFetching: false,
          error: null,
          receivedAt: Date.now(),
          all: action.data
        });
      }
      else {
        // Request initiated
        return Object.assign({}, state, {
          isFetching: true,
          error: null
        });
      }
    default:
      return state;
  }
}
