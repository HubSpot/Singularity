export default function buildApiActionReducer(ActionGroup, initialData=[]) {
  const initialState = {
    isFetching: false,
    error: null,
    receivedAt: null,
    data: initialData
  };

  return function reducer(state = initialState, action) {
    switch (action.type) {
      case ActionGroup.ERROR:
        return Object.assign({}, state, {
          isFetching: false,
          error: action.error
        });
      case ActionGroup.SUCCESS:
        return Object.assign({}, state, {
          isFetching: false,
          error: null,
          receivedAt: Date.now(),
          data: action.data
        });
      case ActionGroup.STARTED:
        // Request initiated
        return Object.assign({}, state, {
          isFetching: true,
          error: null
        });
      default:
        return state;
    }
  }
}