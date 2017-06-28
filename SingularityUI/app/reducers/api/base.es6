const identity = (a) => a;

export default function buildApiActionReducer(ActionGroup, initialData = {}, transform = identity) {
  const initialState = {
    isFetching: false,
    error: null,
    statusCode: null,
    receivedAt: null,
    erroredAt: null,
    data: initialData
  };

  return function reducer(state = initialState, action) {
    let newData = {};

    switch (action.type) {

      case ActionGroup.CLEAR:
        return initialState;

      case ActionGroup.ERROR:
        newData = _.extend({}, state, {
          isFetching: false,
          error: action.error,
          statusCode: action.statusCode,
          erroredAt: Date.now()
        });
        return _.extend({}, state, newData);

      case ActionGroup.SUCCESS:
        newData = _.extend({}, state, {
          isFetching: false,
          error: null,
          statusCode: action.statusCode,
          receivedAt: Date.now(),
          data: transform(action.data)
        });
        return _.extend({}, state, newData);

      case ActionGroup.STARTED:
        newData = _.extend({}, state, {
          isFetching: true
        });
        return _.extend({}, state, newData);

      default:
        return state;
    }
  };
}
