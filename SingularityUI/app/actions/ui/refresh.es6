export const CANCEL_AUTO_REFRESH = 'CANCEL_AUTO_REFRESH';

export const CancelAutoRefresh = (key, isExpired = false) => {
  return (dispatch, getState) => {
    const state = getState();
    if (!state.ui.refresh.hasOwnProperty(key)) {
      // don't beat dead horse, it's already gone
      return;
    }

    const intervalId = state.ui.refresh[key].intervalId;

    // clear automatic expiration if this is the expiration notice
    if (!isExpired) {
      const timeoutId = state.ui.refresh[key].timeoutId;
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    }

    // clear the actual refresh
    clearInterval(intervalId);

    dispatch({
      key,
      type: CANCEL_AUTO_REFRESH
    });
  };
};

export const BEGIN_AUTO_REFRESH = 'BEGIN_AUTO_REFRESH';

export const BeginAutoRefresh = (key, actions, interval = 30000, expiration = null) => {
  return (dispatch) => {
    const intervalId = setInterval(
      () => actions.forEach((action) => dispatch(action)),
      interval
    );

    let timeoutId;

    if (expiration) {
      timeoutId = setTimeout(() => dispatch(CancelAutoRefresh(key, true)), expiration);
    }

    dispatch({
      key,
      intervalId,
      timeoutId,
      type: BEGIN_AUTO_REFRESH
    });
  };
};
