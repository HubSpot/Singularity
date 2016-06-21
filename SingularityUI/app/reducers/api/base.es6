export default function buildApiActionReducer(ActionGroup, keyed=false, initialData={}) {

  const initialState = keyed ? {} : {
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
        if (keyed) {
          newData[action.key] = _.extend({}, state[action.key], { isFetching: false, error: action.error });
        } else {
          newData = _.extend({}, state, {isFetching: false, error: action.error});
        }
        return _.extend(state, newData);

      case ActionGroup.SUCCESS:
        if (keyed) {
          newData[action.key] = _.extend({}, state[action.key], {
            isFetching: false,
            error: null,
            receivedAt: Date.now(),
            data: action.data
          });
        } else {
          newData = _.extend({}, state, {
            isFetching: false,
            error: null,
            receivedAt: Date.now(),
            data: action.data
          });
        }

        return _.extend(state, newData);

      case ActionGroup.STARTED:
        if (keyed) {
          newData[action.key] = _.extend({}, state[action.key], {
            isFetching: true,
            error: null
          });
        } else {
          newData = _.extend({}, state, {
            isFetching: true,
            error: null
          });
        }
        return _.extend(state, newData);

      default:
        return state;
    }
  }
}
