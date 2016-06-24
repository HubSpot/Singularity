export default function buildApiActionReducer(ActionGroup, initialData={}) {

  const initialState = {
    isFetching: false,
    error: null,
    receivedAt: null,
    data: initialData
  };

  return function reducer(state = initialState, action) {
    let newData = {};

    switch (action.type) {

      case ActionGroup.CLEAR:
        return initialState;

      case ActionGroup.ERROR:
        newData = _.extend({}, state, {isFetching: false, error: action.error});
        return _.extend({}, state, newData);

      case ActionGroup.SUCCESS:
        newData = _.extend({}, state, {
          isFetching: false,
          error: null,
          receivedAt: Date.now(),
          data: action.data
        });
        return _.extend({}, state, newData);

      case ActionGroup.STARTED:
        newData = _.extend({}, state, {
          isFetching: true,
          error: null
        });
        return _.extend({}, state, newData);

      default:
        return state;
    }
  }
}
