import * as RefreshActions from '../../actions/ui/refresh';

const refresh = (state = {}, action) => {
  if (action.type === RefreshActions.BEGIN_AUTO_REFRESH) {
    if (state.hasOwnProperty(action.key)) {
      // OK WOW NOT OKAY
      console.error(`Key ${action.key} was used twice in auto refresh.`); // eslint-disable-line no-console
    }

    const { key, intervalId, timeoutId } = action;

    const newState = {
      ...state,
      [key]: {
        intervalId,
        timeoutId
      }
    };

    return newState;
  } else if (action.type === RefreshActions.CANCEL_AUTO_REFRESH) {
    const { key } = action;

    const newState = {
      ...state,
    };

    delete newState[key];

    return newState;
  }

  return state;
};

export default refresh;
