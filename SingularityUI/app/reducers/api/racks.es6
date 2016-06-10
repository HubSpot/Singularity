import * as RacksActions from '../../actions/api/racks';

const initialState = {
  isFetching: false,
  error: null,
  receivedAt: null,
  data: []
};

export default function slaves(state = initialState, action) {
  switch (action.type) {
    case RacksActions.FETCH_RACKS_ERROR:
      return Object.assign({}, state, {
        isFetching: false,
        error: action.error
      });
    case RacksActions.FETCH_RACKS_SUCCESS:
      return Object.assign({}, state, {
        isFetching: false,
        error: null,
        receivedAt: Date.now(),
        data: action.data
      });
    case RacksActions.FETCH_RACKS_STARTED:
      // Request initiated
      return Object.assign({}, state, {
        isFetching: true,
        error: null
      });
    default:
      return state;
  }
}