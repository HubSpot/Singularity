export default function buildApiActionReducer(ActionGroup, initialData = []) {
  const initialState = {
    isFetching: false,
    error: null,
    receivedAt: null,
    data: initialData
  };

  return function reducer(state = initialState, action) {
    switch (action.type) {
      case ActionGroup.CLEAR:
        return initialState;
      case ActionGroup.ERROR:
        return _.extend({}, state, {
          isFetching: false,
          error: action.ex,
          receivedAt: Date.now()
        });
      case ActionGroup.SUCCESS:
        return _.extend({}, state, {
          isFetching: false,
          error: null,
          receivedAt: Date.now(),
          data: action.data
        });
      case ActionGroup.STARTED:
        // Request initiated
        return _.extend({}, state, {
          isFetching: true,
          error: null
        });
      default:
        return state;
    }
  };
}
